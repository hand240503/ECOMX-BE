package com.ndh.ShopTechnology.services.user.impl;

import com.ndh.ShopTechnology.constants.MessageConstant;
import com.ndh.ShopTechnology.constants.SystemConstant;
import com.ndh.ShopTechnology.dto.request.PaginationRequest;
import com.ndh.ShopTechnology.dto.request.user.CreateUserRequest;
import com.ndh.ShopTechnology.dto.request.user.ChangePasswordRequest;
import com.ndh.ShopTechnology.dto.request.user.ChangeContactRequest;
import com.ndh.ShopTechnology.dto.request.user.AdminModUserInfoRequest;
import com.ndh.ShopTechnology.dto.request.user.ModUserInfoRequest;

import com.ndh.ShopTechnology.dto.response.user.ChangePasswordResponse;
import com.ndh.ShopTechnology.dto.response.user.LoginResponse;
import com.ndh.ShopTechnology.dto.response.user.UserResponse;
import com.ndh.ShopTechnology.entities.role.RoleEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.entities.user.UserInfoEntity;
import com.ndh.ShopTechnology.exception.AuthenticationFailedException;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.RoleRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.auth.TokenFacade;
import com.ndh.ShopTechnology.services.permission.PermissionService;
import com.ndh.ShopTechnology.services.user.RoleAssignmentService;
import com.ndh.ShopTechnology.services.user.UserService;
import com.ndh.ShopTechnology.services.user.helper.UserValidationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;
    private final UserValidationHelper userValidationHelper;
    private final RoleAssignmentService roleAssignmentService;
    private final TokenFacade tokenFacade;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_REGEX = "\\d{10,15}";

    /* ================== Helpers ================== */

    @Transactional(readOnly = true)
    protected UserEntity getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationFailedException(MessageConstant.AUTH_FAILED);
        }
        String username = auth.getName();
        return userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new AuthenticationFailedException(MessageConstant.AUTH_FAILED));
    }

    protected boolean isAdmin(UserEntity user) {
        return user != null && user.hasAnyPermission("admin:all");
    }

    /**
     * Load user directly from database for write flows to avoid detached entities.
     * Cache is intentionally skipped because JPA must track this entity in current session.
     */
    @Transactional(readOnly = true)
    protected UserEntity loadUserForWrite(Long id) {
        if (id == null) {
            throw new NotFoundEntityException(MessageConstant.USER_NOT_FOUND);
        }
        return userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new NotFoundEntityException(
                        String.format(MessageConstant.USER_NOT_FOUND_BY_ID, id)));
    }

    /**
     * Load user from cache for read-only flows.
     */
    @Cacheable(value = "users", key = "#id")
    @Transactional(readOnly = true)
    public UserEntity loadUser(Long id) {
        if (id == null) {
            throw new NotFoundEntityException(MessageConstant.USER_NOT_FOUND);
        }
        return userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new NotFoundEntityException(
                        String.format(MessageConstant.USER_NOT_FOUND_BY_ID, id)));
    }

    protected UserResponse toResponse(UserEntity user) {
        return UserResponse.fromEntity(user);
    }

    /**
     * Update profile fields shared by both normal users and admins.
     */
    protected void applyProfileFields(UserEntity user, AdminModUserInfoRequest req) {
        if (req == null || user == null)
            return;

        UserInfoEntity userInfo = user.getUserInfo();
        if (userInfo == null) {
            userInfo = new UserInfoEntity();
            userInfo.setUser(user);
            user.setUserInfo(userInfo);
        }

        if (req.getFullName() != null)
            userInfo.setFullName(req.getFullName());
        if (req.getTelephone() != null)
            userInfo.setTelephone(req.getTelephone());
        if (req.getAvatar() != null)
            userInfo.setAvatar(req.getAvatar());
        if (req.getManagerId() != null)
            userInfo.setManagerId(req.getManagerId());
        if (req.getInfo01() != null)
            userInfo.setInfo01(req.getInfo01());
        if (req.getInfo02() != null)
            userInfo.setInfo02(req.getInfo02());
        if (req.getInfo03() != null)
            userInfo.setInfo03(req.getInfo03());
        if (req.getInfo04() != null)
            userInfo.setInfo04(req.getInfo04());
    }

    /**
     * Update sensitive fields for admin-only flows.
     */
    protected void applyAdminFields(UserEntity user, AdminModUserInfoRequest req) {
        if (req == null || user == null)
            return;

        if (req.getEmail() != null)
            user.setEmail(req.getEmail());
        if (req.getPhoneNumber() != null)
            user.setPhoneNumber(req.getPhoneNumber());
        if (req.getStatus() != null)
            user.setStatus(req.getStatus());
        if (req.getType() != null)
            user.setType(req.getType());

        if (StringUtils.hasText(req.getPassword())) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        if (req.getRoleIds() != null && !req.getRoleIds().isEmpty()) {
            List<RoleEntity> roleList = roleRepository.findAllById(Objects.requireNonNull(req.getRoleIds()));
            Set<RoleEntity> roles = new HashSet<>(roleList);
            if (!roles.isEmpty()) {
                user.setRoles(roles);
            } else {
                log.warn("No valid roles found for roleIds: {}", req.getRoleIds());
            }
        }
    }

    /**
     * Core method for user update.
     * Admin can update any user and all fields.
     * Non-admin can only update profile fields of their own account.
     */
    @Transactional
    protected UserResponse doUpdateUser(AdminModUserInfoRequest req, UserEntity actor) {
        if (req == null || req.getId() == null) {
            throw new NotFoundEntityException(MessageConstant.USER_NOT_FOUND);
        }
        if (actor == null) {
            throw new AuthenticationFailedException(MessageConstant.AUTH_FAILED);
        }

        final UserEntity target;

        if (isAdmin(actor)) {
            // Load directly from DB so JPA tracks entity in current session.
            target = loadUserForWrite(req.getId());
            applyProfileFields(target, req);
            applyAdminFields(target, req);
        } else {
            // Non-admin can update only their own profile.
            if (!req.getId().equals(actor.getId())) {
                throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
            }
            // Actor is already managed in current session.
            target = actor;
            applyProfileFields(target, req);
        }

        UserEntity saved = userRepository.save(Objects.requireNonNull(target));
        return toResponse(saved);
    }

    /* ================== Services ================== */

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(PaginationRequest request) {
        // Permission check must run before paging query.
        UserEntity currentUser = getCurrentUserEntity();
        if (!currentUser.hasAnyPermission("user:read", "admin:all")) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }

        int page = (request.getPage() == null) ? 0 : request.getPage();
        int size = (request.getSize() == null) ? 10 : request.getSize();

        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> userPage = userRepository.findAll(pageable);

        return userPage.map(UserResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserEntity getCurrentUser() {
        return getCurrentUserEntity();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserInfo(Long id) {
        UserEntity currentUser = getCurrentUser();

        if (!currentUser.hasAnyPermission("user:read", "admin:all")) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }

        return toResponse(loadUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile() {
        return toResponse(getCurrentUserEntity());
    }

    @Override
    @Transactional
    @CacheEvict(value = { "users", "userPermissions" }, key = "#req.id")
    public UserResponse updateUserInfo(AdminModUserInfoRequest req) {
        if (req == null || req.getId() == null) {
            throw new NotFoundEntityException(MessageConstant.USER_NOT_FOUND);
        }

        UserEntity actor = getCurrentUserEntity();

        // Read target username before update to avoid repopulating evicted cache.
        boolean isTargetDifferentFromActor = isAdmin(actor) && !req.getId().equals(actor.getId());
        String targetUsername = null;
        if (isTargetDifferentFromActor) {
            targetUsername = userRepository.findUsernameById(req.getId())
                    .orElseThrow(() -> new NotFoundEntityException(
                            String.format(MessageConstant.USER_NOT_FOUND_BY_ID, req.getId())));
        }

        UserResponse response = doUpdateUser(req, actor);

        // Clear permission cache for actor.
        permissionService.clearUserPermissionsCache(actor.getUsername());

        // Clear permission cache for target user when different from actor.
        if (isTargetDifferentFromActor && targetUsername != null) {
            permissionService.clearUserPermissionsCache(targetUsername);
        }

        log.info("User updated: actorId={}, targetId={}", actor.getId(), req.getId());
        return response;
    }

    @Override
    @Transactional
    public UserResponse updateProfileInfo(ModUserInfoRequest req) {
        UserEntity currentUser = getCurrentUserEntity();

        // Reuse central update flow by converting to admin request shape.
        AdminModUserInfoRequest adminReq = AdminModUserInfoRequest.builder()
                .id(currentUser.getId())
                .fullName(req != null ? req.getFullName() : null)
                .telephone(req != null ? req.getTelephone() : null)
                .avatar(req != null ? req.getAvatar() : null)
                .managerId(req != null ? req.getManagerId() : null)
                .info01(req != null ? req.getInfo01() : null)
                .info02(req != null ? req.getInfo02() : null)
                .info03(req != null ? req.getInfo03() : null)
                .info04(req != null ? req.getInfo04() : null)
                .build();

        UserResponse response = doUpdateUser(adminReq, currentUser);

        permissionService.clearUserPermissionsCache(currentUser.getUsername());

        log.info("Profile updated: username={}", currentUser.getUsername());
        return response;
    }

    @Override
    public ChangePasswordResponse changePassword(ChangePasswordRequest request) {
        if (request == null) {
            return ChangePasswordResponse.of(false, HttpStatus.BAD_REQUEST, "Yêu cầu đổi mật khẩu không hợp lệ");
        }

        UserEntity currentUser;
        try {
            currentUser = getCurrentUserEntity();
        } catch (AuthenticationFailedException ex) {
            return ChangePasswordResponse.of(false, HttpStatus.UNAUTHORIZED, MessageConstant.AUTH_FAILED);
        } catch (Exception ex) {
            log.error("Cannot resolve current user for password change", ex);
            return ChangePasswordResponse.of(false, HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xác định người dùng hiện tại");
        }

        String currentPassword = request.getCurrentPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        if (!StringUtils.hasText(currentPassword) || !StringUtils.hasText(newPassword)
                || !StringUtils.hasText(confirmPassword)) {
            return ChangePasswordResponse.of(false, HttpStatus.BAD_REQUEST, "Thông tin mật khẩu không hợp lệ");
        }

        if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
            return ChangePasswordResponse.of(false, HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }

        if (!newPassword.equals(confirmPassword)) {
            return ChangePasswordResponse.of(false, HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận không khớp");
        }

        if (!newPassword.equals(newPassword.trim()) || !confirmPassword.equals(confirmPassword.trim())) {
            return ChangePasswordResponse.of(false, HttpStatus.BAD_REQUEST, "Mật khẩu mới không được có khoảng trắng ở đầu hoặc cuối");
        }

        if (newPassword.length() < 6) {
            return ChangePasswordResponse.of(false, HttpStatus.BAD_REQUEST, "Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        if (passwordEncoder.matches(newPassword, currentUser.getPassword())) {
            return ChangePasswordResponse.of(false, HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        try {
            currentUser.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(currentUser);

            permissionService.clearUserPermissionsCache(currentUser.getUsername());
            log.info("Password changed: username={}", currentUser.getUsername());
            return ChangePasswordResponse.of(true, HttpStatus.OK, "Đổi mật khẩu thành công");
        } catch (Exception ex) {
            log.error("Change password failed: username={}", currentUser.getUsername(), ex);
            return ChangePasswordResponse.of(false, HttpStatus.INTERNAL_SERVER_ERROR, "Đổi mật khẩu thất bại");
        }
    }

    @Override
    @Transactional
    public LoginResponse changeContact(ChangeContactRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ChangeContactRequest cannot be null");
        }

        UserEntity currentUser = getCurrentUserEntity();

        String currentPassword = request.getCurrentPassword();
        if (!StringUtils.hasText(currentPassword) || !passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }

        String email = request.getEmail();
        String phone = request.getPhoneNumber();

        boolean hasEmail = StringUtils.hasText(email);
        boolean hasPhone = StringUtils.hasText(phone);
        if (!hasEmail && !hasPhone) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Email hoặc số điện thoại không được để trống");
        }

        if (hasEmail) {
            String normalizedEmail = email.trim().toLowerCase();
            if (!normalizedEmail.matches(EMAIL_REGEX)) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST, "Email không hợp lệ");
            }

            String currentEmail = currentUser.getEmail();
            if (currentEmail == null || !normalizedEmail.equalsIgnoreCase(currentEmail)) {
                if (userRepository.existsByEmailAndIdNot(normalizedEmail, currentUser.getId())) {
                    throw new CustomApiException(HttpStatus.CONFLICT, "Email này đã được đăng ký");
                }
                currentUser.setEmail(normalizedEmail);
            }
        }

        if (hasPhone) {
            String normalizedPhone = phone.trim();
            if (!normalizedPhone.matches(PHONE_REGEX)) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST, "Số điện thoại không hợp lệ");
            }

            String currentPhone = currentUser.getPhoneNumber();
            if (currentPhone == null || !normalizedPhone.equals(currentPhone)) {
                if (userRepository.existsByPhoneNumberAndIdNot(normalizedPhone, currentUser.getId())) {
                    throw new CustomApiException(HttpStatus.CONFLICT, "Số điện thoại này đã được đăng ký");
                }
                currentUser.setPhoneNumber(normalizedPhone);
            }
        }

        UserEntity saved = userRepository.save(currentUser);

        // Clear permission cache before returning refreshed login tokens.
        permissionService.clearUserPermissionsCache(saved.getUsername());

        log.info("Contact changed: userId={}, username={}", saved.getId(), saved.getUsername());
        return tokenFacade.issueLoginResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateUserRequest cannot be null");
        }

        UserEntity currentUser = getCurrentUserEntity();
        if (!currentUser.hasAnyPermission("user:create", "admin:all")) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }

        String username = userValidationHelper.validateAndNormalizeUsername(request.getUsername());
        String phone = userValidationHelper.validateAndNormalizePhone(request.getPhoneNumber());
        String password = userValidationHelper.validatePassword(request.getPassword());

        if (userRepository.existsByUsername(username)) {
            throw new CustomApiException(HttpStatus.CONFLICT, "Username already exists: " + username);
        }
        if (userRepository.existsByPhoneNumber(phone)) {
            throw new CustomApiException(HttpStatus.CONFLICT, "Phone number already exists: " + phone);
        }

        UserEntity user = UserEntity.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .phoneNumber(phone)
                .status(SystemConstant.ACTIVE_STATUS)
                .build();

        UserInfoEntity userInfo = UserInfoEntity.builder()
                .user(user)
                .fullName(request.getFullName() != null ? request.getFullName().trim() : null)
                .telephone(request.getTelephone() != null ? request.getTelephone().trim() : null)
                .avatar(request.getAvatar())
                .managerId(request.getManagerId() != null ? request.getManagerId() : currentUser.getId())
                .info01(request.getInfo01())
                .info02(request.getInfo02())
                .info03(request.getInfo03())
                .info04(request.getInfo04())
                .build();

        user.setUserInfo(userInfo);

        Set<RoleEntity> roles = roleAssignmentService.assignRoles(request.getRoleIds());
        user.setRoles(roles);

        user = userRepository.save(user);

        log.info("User created: id={}, username={}, roles={}",
                user.getId(), user.getUsername(), roles.stream().map(RoleEntity::getCode).toList());

        return toResponse(user);
    }
}