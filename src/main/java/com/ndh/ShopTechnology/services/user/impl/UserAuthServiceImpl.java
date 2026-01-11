package com.ndh.ShopTechnology.services.user.impl;

import com.ndh.ShopTechnology.config.TokenProvider;
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
import com.ndh.ShopTechnology.services.token.RefreshTokenService;
import com.ndh.ShopTechnology.services.user.UserAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TokenProvider jwtTokenUtil;
    private final OTPService otpService;
    private final RefreshTokenService  refreshTokenService;

    @Override
    @Transactional
    public LoginResponse registerUser(RegisterUserRequest request) {
        // ==================== VALIDATION ====================
        String email = validateAndNormalizeEmail(request.getEmail());
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
        boolean verified = otpService.verifyOTPForRegister(email, verificationCode);

        if (!verified) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Mã xác thực không hợp lệ hoặc đã hết hạn"
            );
        }

        // ==================== CHECK USER EXISTS ====================
        checkUserNotExists(email);

        // ==================== LOAD DEFAULT ROLE ====================
        RoleEntity role = loadRole("CUSTOMER");

        // ==================== DETERMINE USERNAME & PHONE ====================
        String username = email;
        String phone = isValidPhone(email) ? email : null;
        String emailValue = isValidEmail(email) ? email : null;

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
                .firstName(null)
                .lastName(null)
                .build();

        user.setUserInfo(userInfo);

        // ==================== SET ROLES ====================
        Set<RoleEntity> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        // ==================== SAVE USER ====================
        user = userRepository.save(user);

        // ==================== CLEAR OTP ====================
        otpService.clearOTP(email);

        log.info("User registered successfully: username={}, email={}", username, emailValue);

        // ==================== GENERATE TOKEN ====================
        // Authenticate user sau khi đăng ký
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        String token = jwtTokenUtil.generateToken(authentication);

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

        // ✅ Generate Access Token
        String accessToken = jwtTokenUtil.generateAccessToken(authentication);

        // ✅ Generate Refresh Token và lưu vào DB
        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        UserResponse userResponse = UserResponse.fromEntity(user);

        log.info("User logged in successfully: username={}", user.getUsername());

        return LoginResponse.builder()
                .userInfo(userResponse)
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(900L) // 15 minutes in seconds
                .build();
    }

    // ==================== HELPER METHODS - VALIDATION ====================

    private String validateAndNormalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Email hoặc số điện thoại không được để trống"
            );
        }

        String normalized = email.trim().toLowerCase();

        if (!isValidEmail(normalized) && !isValidPhone(normalized)) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Email hoặc số điện thoại không hợp lệ"
            );
        }

        return normalized;
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
        boolean exists = isValidEmail(email)
                ? userRepository.existsByUsername(email)
                : userRepository.existsByPhoneNumber(email);

        if (exists) {
            throw new CustomApiException(
                    HttpStatus.CONFLICT,
                    isValidEmail(email)
                            ? "Email này đã được đăng ký"
                            : "Số điện thoại này đã được đăng ký"
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

        Optional<UserEntity> userOpt = isPhone
                ? userRepository.findOneByPhoneNumber(login)
                : userRepository.findOneByUsername(login.toLowerCase());

        if (userOpt.isPresent()) {
            UserEntity basicUser = userOpt.get();

            // Load đầy đủ thông tin user (roles, permissions, userInfo, addresses)
            return userRepository.findByUsernameWithRolesAndPermissions(basicUser.getUsername())
                    .orElseThrow(() -> new AuthenticationFailedException(MessageConstant.AUTH_FAILED));
        }

        throw new AuthenticationFailedException(MessageConstant.AUTH_FAILED);
    }

    // ✅ THÊM: Helper methods để validate email và phone
    private boolean isValidEmail(String input) {
        return input != null && input.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidPhone(String input) {
        return input != null && input.matches("\\d{10,15}");
    }

    private boolean isPhoneNumber(String input) {
        return input != null && input.matches("\\d{10,15}");
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(String refreshTokenValue) {
        // Verify refresh token
        RefreshTokenEntity refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);

        UserEntity user = refreshToken.getUser();

        // Generate new access token
        String newAccessToken = jwtTokenUtil.generateAccessToken(user.getUsername());

        // Optional: Generate new refresh token (rotate)
        RefreshTokenEntity newRefreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        UserResponse userResponse = UserResponse.fromEntity(user);

        log.info("Token refreshed for user: {}", user.getUsername());

        return LoginResponse.builder()
                .userInfo(userResponse)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(900L)
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