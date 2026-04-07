package com.ndh.ShopTechnology.services.user.impl;

import com.ndh.ShopTechnology.constant.MessageConstant;
import com.ndh.ShopTechnology.constant.SystemConstant;
import com.ndh.ShopTechnology.dto.request.auth.LoginRequest;
import com.ndh.ShopTechnology.dto.request.auth.RegisterUserRequest;
import com.ndh.ShopTechnology.dto.response.user.LoginResponse;
import com.ndh.ShopTechnology.dto.response.user.UserResponse;
import com.ndh.ShopTechnology.entities.role.RoleEntity;
import com.ndh.ShopTechnology.entities.token.RefreshTokenEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.entities.user.UserInfoEntity;
import com.ndh.ShopTechnology.exception.AuthenticationFailedException;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.RoleRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.otp.OTPService;
import com.ndh.ShopTechnology.services.auth.JwtService;
import com.ndh.ShopTechnology.services.token.RefreshTokenService;
import com.ndh.ShopTechnology.services.user.CustomUserDetailsService;
import com.ndh.ShopTechnology.services.user.UserAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final OTPService otpService;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;

    private static final int RANDOM_USERNAME_LENGTH = 10;
    private static final String USERNAME_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String PHONE_USERNAME_SUFFIX_CHARS = "#$@_";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    @Transactional
    public LoginResponse registerUser(RegisterUserRequest request) {
        // ==================== VALIDATION ====================
        String login = validateAndNormalizeLogin(request.getEmail());
        String password = validatePassword(request.getPassword());
        String confirmPassword = request.getConfirmPassword();
        String verificationCode = request.getVerificationCode();

        // Check password match
        if (!password.equals(confirmPassword)) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu xác nhận không khớp"
            );
        }

        // ==================== VERIFY OTP ====================
        boolean verified = otpService.verifyOTPForRegister(login, verificationCode);

        if (!verified) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Mã xác thực không hợp lệ hoặc đã hết hạn"
            );
        }

        // ==================== CHECK USER EXISTS ====================
        checkUserNotExists(login);

        // ==================== LOAD DEFAULT ROLE ====================
        RoleEntity role = loadRole("CUSTOMER");

        // ==================== DETERMINE USERNAME / EMAIL / PHONE ====================
        boolean isPhone = isValidPhone(login);
        boolean isEmail = isValidEmail(login);

        String emailValue = isEmail ? login : null;
        String phone = isPhone ? login : null;

        // Rules:
        // 1) If user registers by username (not email/phone): keep username as-is
        // 2) If user registers by email: generate random username (10 chars), derived from local-part before '@'
        // 3) Phone case: keep current behavior (username = phone) until you define another rule
        final String username;
        if (!isEmail && !isPhone) {
            username = login;
        } else if (isEmail) {
            username = generateUniqueUsernameFromEmail(login);
        } else {
            username = generateUniqueUsernameForPhone();
        }

        // ==================== BUILD USER ENTITY ====================
        UserEntity user = UserEntity.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(emailValue)
                .phoneNumber(phone)
                .status(SystemConstant.ACTIVE_STATUS)
                .build();

        // ==================== BUILD USER INFO ENTITY ====================
        UserInfoEntity userInfo = UserInfoEntity.builder()
                .user(user)
                .fullName(null)
                .build();

        user.setUserInfo(userInfo);

        // ==================== SET ROLES ====================
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        // ==================== SAVE USER ====================
        user = userRepository.save(user);

        // ==================== CLEAR OTP ====================
        otpService.clearOTP(login);

        log.info("User registered successfully: username={}, email={}", username, emailValue);

        // ==================== GENERATE TOKEN ====================
        // Authenticate user sau khi đăng ký
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ==================== BUILD RESPONSE ====================
        UserResponse userResponse = UserResponse.fromEntity(user);

        return LoginResponse.builder()
                .userInfo(userResponse)
                .build();
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String login = validateLoginInput(request.getLogin());
        String password = request.getPassword();

        // Load user với đầy đủ thông tin
        UserEntity user = loadUserForLogin(login);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Failed login attempt for user: {}", login);
            throw new AuthenticationFailedException(MessageConstant.AUTH_FAILED);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtService.generateAccessToken(authentication.getName());

        RefreshTokenEntity refreshToken = refreshTokenService.createInitialRefreshToken(user.getUsername(), null);

        UserResponse userResponse = UserResponse.fromEntity(user);

        log.info("User logged in successfully: username={}", user.getUsername());

        return LoginResponse.builder()
                .userInfo(userResponse)
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .build();
    }

    // ==================== HELPER METHODS - VALIDATION ====================

    private String validateAndNormalizeLogin(String login) {
        if (login == null || login.trim().isEmpty()) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Email / số điện thoại / username không được để trống"
            );
        }

        return login.trim().toLowerCase();
    }

    private String validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu không được để trống"
            );
        }

        if (password.length() < 6) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu phải có ít nhất 6 ký tự"
            );
        }

        return password;
    }

    private String normalizeRoleCode(String roleCode) {
        String normalized = Optional.ofNullable(roleCode)
                .map(String::trim)
                .map(String::toUpperCase)
                .orElse("USER");

        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        return normalized;
    }

    private String validateLoginInput(String login) {
        return Optional.ofNullable(login)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElseThrow(() -> new AuthenticationFailedException(MessageConstant.AUTH_FAILED));
    }

    // ==================== HELPER METHODS - DATABASE ====================

    private void checkUserNotExists(String email) {
        boolean isEmail = isValidEmail(email);
        boolean isPhone = isValidPhone(email);

        boolean exists;
        if (isEmail) {
            exists = userRepository.existsByEmail(email);
        } else if (isPhone) {
            exists = userRepository.existsByPhoneNumber(email);
        } else {
            exists = userRepository.existsByUsername(email);
        }

        if (exists) {
            throw new CustomApiException(
                    HttpStatus.CONFLICT,
                    isEmail
                            ? "Email này đã được đăng ký"
                            : isPhone
                            ? "Số điện thoại này đã được đăng ký"
                            : "Username này đã được đăng ký"
            );
        }
    }

    private RoleEntity loadRole(String roleCode) {
        String normalizedRoleCode = normalizeRoleCode(roleCode);
        return roleRepository.findByCode(normalizedRoleCode)
                .orElseThrow(() -> new NotFoundEntityException(
                        "Role not found: " + normalizedRoleCode + ". Please check role configuration."
                ));
    }

    private UserEntity loadUserForLogin(String login) {

        boolean isPhone = isPhoneNumber(login);
        boolean isEmail = isValidEmail(login);

        Optional<UserEntity> userOpt;
        if (isPhone) {
            userOpt = userRepository.findOneByPhoneNumber(login);
        } else if (isEmail) {
            userOpt = userRepository.findOneByEmail(login.toLowerCase());
        } else {
            // Username/internal login (không phải email/phone)
            userOpt = userRepository.findOneByUsername(login.toLowerCase());
        }

        if (userOpt.isPresent()) {
            UserEntity basicUser = userOpt.get();

            // Load đầy đủ thông tin user (roles, permissions, userInfo, addresses)
            return userRepository.findByUsernameWithRolesAndPermissions(basicUser.getUsername())
                    .orElseThrow(() -> new AuthenticationFailedException(MessageConstant.AUTH_FAILED));
        }

        throw new AuthenticationFailedException(MessageConstant.AUTH_FAILED);
    }

    private boolean isValidEmail(String input) {
        return input != null && input.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidPhone(String input) {
        return input != null && input.matches("\\d{10,15}");
    }

    private String generateUniqueUsernameFromEmail(String email) {
        String localPart = email.substring(0, email.indexOf('@'));
        String base = localPart.replaceAll("[^a-z0-9]", "");
        if (base.isBlank()) {
            base = "user";
        }

        // Keep base short to make room for random suffix
        if (base.length() > 6) {
            base = base.substring(0, 6);
        }

        for (int i = 0; i < 50; i++) {
            String candidate = base + randomString(RANDOM_USERNAME_LENGTH - base.length());
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        // Fallback: fully random
        for (int i = 0; i < 50; i++) {
            String candidate = randomString(RANDOM_USERNAME_LENGTH);
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        throw new CustomApiException(HttpStatus.CONFLICT, "Không thể tạo username hợp lệ. Vui lòng thử lại.");
    }

    private String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(USERNAME_CHARS.charAt(SECURE_RANDOM.nextInt(USERNAME_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Phone registration rule:
     * - Username length = 11
     * - First 10 chars: [a-z0-9] random
     * - 11th char: one of "#$@_"
     */
    private String generateUniqueUsernameForPhone() {
        for (int i = 0; i < 100; i++) {
            String candidate = randomString(RANDOM_USERNAME_LENGTH)
                    + PHONE_USERNAME_SUFFIX_CHARS.charAt(SECURE_RANDOM.nextInt(PHONE_USERNAME_SUFFIX_CHARS.length()));
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        throw new CustomApiException(HttpStatus.CONFLICT, "Không thể tạo username hợp lệ. Vui lòng thử lại.");
    }

    private boolean isPhoneNumber(String input) {
        return input != null && input.matches("\\d{10,15}");
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(String refreshTokenValue, String deviceId, String ipAddress, String userAgent) {
        RefreshTokenEntity newRefreshToken = refreshTokenService.rotateRefreshToken(
                refreshTokenValue,
                deviceId,
                ipAddress,
                userAgent
        );

        UserEntity user = newRefreshToken.getUser();

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getUsername());
        Authentication refreshAuthentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        String newAccessToken = jwtService.generateAccessToken(refreshAuthentication.getName());

        UserResponse userResponse = UserResponse.fromEntity(user);

        log.info("Token refreshed for user: {}", user.getUsername());

        return LoginResponse.builder()
                .userInfo(userResponse)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .build();
    }

    @Override
    @Transactional
    public void logout(String username) {
        UserEntity user = userRepository.findOneByUsername(username)
                .orElseThrow(() -> new CustomApiException(
                        HttpStatus.NOT_FOUND,
                        "User không tồn tại"
                ));

        // Revoke all refresh tokens
        refreshTokenService.revokeUserTokens(user);

        log.info("User logged out: {}", username);
    }
}