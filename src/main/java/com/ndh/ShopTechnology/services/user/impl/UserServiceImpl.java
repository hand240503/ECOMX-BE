package com.ndh.ShopTechnology.services.user.impl;

import com.ndh.ShopTechnology.constants.AdminManagedUserKind;
import com.ndh.ShopTechnology.constants.MessageConstant;
import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.constants.RoleConstant;
import com.ndh.ShopTechnology.constants.SystemConstant;
import com.ndh.ShopTechnology.dto.request.PaginationRequest;
import com.ndh.ShopTechnology.dto.request.user.CreateUserRequest;
import com.ndh.ShopTechnology.dto.request.user.ChangePasswordRequest;
import com.ndh.ShopTechnology.dto.request.user.ChangeContactRequest;
import com.ndh.ShopTechnology.dto.request.permission.GrantPermissionRequest;
import com.ndh.ShopTechnology.dto.request.permission.RevokePermissionRequest;
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
import com.ndh.ShopTechnology.services.permission.PermissionEvaluator;
import com.ndh.ShopTechnology.services.permission.PermissionService;
import com.ndh.ShopTechnology.services.permission.RolePermissionService;
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

import java.security.SecureRandom;
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
    private final RolePermissionService rolePermissionService;

    private final SecureRandom secureRandom = new SecureRandom();

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

    /**
     * Coi user là admin nếu có ít nhất 1 trong các quyền hệ thống "*_ALL" (101..104).
     * Đây là tiêu chí dùng cho các flow cập nhật user theo id (admin update bất kỳ user nào).
     */
    protected boolean isAdmin(UserEntity user) {
        if (user == null) return false;
        return PermissionEvaluator.hasAnyPermission(
                user.getAllPermissions(),
                PermissionCode.UPDATE_USER,
                PermissionCode.READ_ALL,
                PermissionCode.CREATE_ALL,
                PermissionCode.DELETE_ALL);
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

    protected UserResponse toResponse(UserEntity user, String temporaryPassword) {
        UserResponse r = UserResponse.fromEntity(user);
        if (temporaryPassword != null) {
            r.setTemporaryPassword(temporaryPassword);
        }
        return r;
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

        if (req.getManId() != null)
            user.setManId(req.getManId());

        if (StringUtils.hasText(req.getPassword())) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        if (req.getRoleId() != null) {
            roleRepository.findById(req.getRoleId()).ifPresentOrElse(
                    user::setRole,
                    () -> log.warn("No valid role found for roleId: {}", req.getRoleId())
            );
        }
    }

    /**
     * Core method for user update.
     * <ul>
     *   <li>Admin (wildcard 101–104) hoặc quyền UPDATE_USER/UPDATE_ALL: chỉnh mọi trường hợp lệ.</li>
     *   <li>Người có {@link PermissionCode#UPDATE_USER}, hoặc nhân viên có {@code man_id} trỏ về actor: chỉ đổi khóa/mở (status).</li>
     *   <li>Còn lại: chỉ sửa profile của chính mình.</li>
     * </ul>
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
        if (req.getId().equals(actor.getId())) {
            target = actor;
            applyProfileFields(target, req);
        } else {
            UserEntity other = loadUserForWrite(req.getId());
            boolean fullMgmt = isAdmin(actor)
                    || PermissionEvaluator.hasAnyPermission(
                    actor.getAllPermissions(),
                    PermissionCode.UPDATE_USER,
                    PermissionCode.UPDATE_ALL);
            if (fullMgmt) {
                applyProfileFields(other, req);
                applyAdminFields(other, req);
                target = other;
            } else if (canApplyAccountLock(actor, other, req)) {
                applyAccountStatusOnly(other, req);
                target = other;
            } else {
                throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
            }
        }

        UserEntity saved = userRepository.save(Objects.requireNonNull(target));
        return toResponse(saved);
    }

    /** Khóa/mở (status): quản lý trực tiếp theo man_id, hoặc có quyền UPDATE_USER — body chỉ gồm id + status. */
    protected boolean canApplyAccountLock(UserEntity actor, UserEntity other, AdminModUserInfoRequest req) {
        if (req.getStatus() == null) {
            return false;
        }
        if (!isAdministrativeLockOnlyRequest(req)) {
            return false;
        }
        if (other.getManId() != null && other.getManId().equals(actor.getId())) {
            return true;
        }
        return PermissionEvaluator.hasAnyPermission(
                actor.getAllPermissions(),
                PermissionCode.LOCK_USER);
    }

    private boolean isAdministrativeLockOnlyRequest(AdminModUserInfoRequest req) {
        if (StringUtils.hasText(req.getPassword())) {
            return false;
        }
        if (req.getEmail() != null) {
            return false;
        }
        if (req.getPhoneNumber() != null) {
            return false;
        }
        if (req.getType() != null) {
            return false;
        }
        if (req.getManId() != null) {
            return false;
        }
        if (req.getRoleId() != null) {
            return false;
        }
        if (req.getFullName() != null) {
            return false;
        }
        if (req.getTelephone() != null) {
            return false;
        }
        if (req.getAvatar() != null) {
            return false;
        }
        if (req.getManagerId() != null) {
            return false;
        }
        if (req.getInfo01() != null || req.getInfo02() != null
                || req.getInfo03() != null || req.getInfo04() != null) {
            return false;
        }
        if (req.getGrantPermissionCodes() != null && !req.getGrantPermissionCodes().isEmpty()) {
            return false;
        }
        if (req.getRevokePermissionCodes() != null && !req.getRevokePermissionCodes().isEmpty()) {
            return false;
        }
        return true;
    }

    private void applyAccountStatusOnly(UserEntity user, AdminModUserInfoRequest req) {
        user.setStatus(req.getStatus());
    }

    /** Admin hoặc SUPER_ADMIN: có thể tạo tài khoản với mọi role. Ngược lại: chỉ MANAGER/EMPLOYEE cho nhân viên. */
    protected boolean isPrivilegedAccountCreator(UserEntity actor) {
        return actor.hasRole(RoleConstant.ROLE_ADMIN)
                || actor.hasRole(RoleConstant.ROLE_SUPER_ADMIN);
    }

    /* ================== Services ================== */

    @Override
    @Transactional(readOnly = true)
    public UserEntity getCurrentUser() {
        return getCurrentUserEntity();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getStaffUsers(PaginationRequest request) {
        UserEntity currentUser = getCurrentUserEntity();
        if (!PermissionEvaluator.hasAnyPermission(currentUser.getAllPermissions(),
                PermissionCode.READ_USER, PermissionCode.READ_ALL)) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }

        int page = (request.getPage() == null) ? 0 : request.getPage();
        int size = (request.getSize() == null) ? 10 : request.getSize();
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> userPage = userRepository.findPagedNonCustomerUsers(
                RoleConstant.ROLE_CUSTOMER, pageable);
        return userPage.map(UserResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getEmployeeUsers(PaginationRequest request) {
        UserEntity currentUser = getCurrentUserEntity();
        if (!PermissionEvaluator.hasAnyPermission(currentUser.getAllPermissions(),
                PermissionCode.READ_USER, PermissionCode.READ_ALL)) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }

        int page = (request.getPage() == null) ? 0 : request.getPage();
        int size = (request.getSize() == null) ? 10 : request.getSize();
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> userPage = userRepository.findByRole_Code(RoleConstant.ROLE_EMPLOYEE, pageable);
        return userPage.map(UserResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getCustomerUsers(PaginationRequest request) {
        UserEntity currentUser = getCurrentUserEntity();
        if (!PermissionEvaluator.hasAnyPermission(currentUser.getAllPermissions(),
                PermissionCode.READ_USER, PermissionCode.READ_ALL)) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }

        int page = (request.getPage() == null) ? 0 : request.getPage();
        int size = (request.getSize() == null) ? 10 : request.getSize();
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> userPage = userRepository.findByRole_Code(RoleConstant.ROLE_CUSTOMER, pageable);
        return userPage.map(UserResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCustomerUser(Long id) {
        UserEntity currentUser = getCurrentUserEntity();
        if (!PermissionEvaluator.hasAnyPermission(currentUser.getAllPermissions(),
                PermissionCode.READ_USER, PermissionCode.READ_ALL)) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }
        UserEntity user = loadUser(id);
        if (user.getRole() == null || !RoleConstant.ROLE_CUSTOMER.equals(user.getRole().getCode())) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Tài khoản không phải khách hàng (CUSTOMER).");
        }
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsersForAdmin(PaginationRequest request) {
        UserEntity currentUser = getCurrentUserEntity();
        if (!PermissionEvaluator.hasAnyPermission(currentUser.getAllPermissions(),
                PermissionCode.READ_USER, PermissionCode.READ_ALL)) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }

        int page = (request.getPage() == null) ? 0 : request.getPage();
        int size = (request.getSize() == null) ? 10 : request.getSize();
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> userPage = userRepository.findAllPagedForAdmin(pageable);
        return userPage.map(UserResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserForAdmin(Long id) {
        UserEntity currentUser = getCurrentUserEntity();
        if (!PermissionEvaluator.hasAnyPermission(currentUser.getAllPermissions(),
                PermissionCode.READ_USER, PermissionCode.READ_ALL)) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }
        UserEntity user = loadUser(id);
        return toResponse(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = { "users", "userPermissions" }, key = "#req.id")
    public UserResponse updateUserForAdmin(AdminModUserInfoRequest req) {
        return finalizeAdminUpdate(req, AdminManagedUserKind.ANY);
    }

    @Override
    @Transactional
    @CacheEvict(value = { "users", "userPermissions" }, key = "#id")
    public void deleteUserForAdmin(Long id) {
        UserEntity actor = getCurrentUserEntity();
        if (!PermissionEvaluator.hasAnyPermission(actor.getAllPermissions(),
                PermissionCode.DELETE_USER,
                PermissionCode.DELETE_ALL)) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }
        UserEntity target = loadUserForWrite(id);
        assertMatchesAdminKind(target, AdminManagedUserKind.ANY);
        if (target.getId().equals(actor.getId())) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Không thể xóa tài khoản của chính bạn.");
        }
        permissionService.clearUserPermissionsCache(target.getUsername());
        userRepository.delete(target);
        log.info("User deleted (unified admin): id={}, by={}", id, actor.getUsername());
    }

    @Override
    @Transactional
    @CacheEvict(value = { "users", "userPermissions" }, key = "#id")
    public UserResponse resetUserPasswordForAdmin(Long id) {
        UserEntity actor = getCurrentUserEntity();
        if (!PermissionEvaluator.hasAnyPermission(actor.getAllPermissions(),
                PermissionCode.UPDATE_USER,
                PermissionCode.UPDATE_ALL)) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }
        UserEntity target = loadUserForWrite(id);
        assertMatchesAdminKind(target, AdminManagedUserKind.ANY);
        String plain = generateStaffInitialPassword(target.getPhoneNumber());
        target.setPassword(passwordEncoder.encode(plain));
        userRepository.save(target);
        permissionService.clearUserPermissionsCache(target.getUsername());
        log.info("User password reset (unified admin): id={}, by={}", id, actor.getUsername());
        return toResponse(target, plain);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getStaffUser(Long id) {
        return getUserForAdminSegment(id, AdminManagedUserKind.STAFF);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getEmployeeUser(Long id) {
        return getUserForAdminSegment(id, AdminManagedUserKind.EMPLOYEE);
    }

    private UserResponse getUserForAdminSegment(Long id, AdminManagedUserKind kind) {
        UserEntity currentUser = getCurrentUser();
        if (!PermissionEvaluator.hasAnyPermission(currentUser.getAllPermissions(),
                PermissionCode.READ_USER, PermissionCode.READ_ALL)) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }
        UserEntity user = loadUser(id);
        assertMatchesAdminKind(user, kind);
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile() {
        return toResponse(getCurrentUserEntity());
    }

    @Override
    @Transactional
    @CacheEvict(value = { "users", "userPermissions" }, key = "#req.id")
    public UserResponse updateStaffUser(AdminModUserInfoRequest req) {
        return finalizeAdminUpdate(req, AdminManagedUserKind.STAFF);
    }

    @Override
    @Transactional
    @CacheEvict(value = { "users", "userPermissions" }, key = "#req.id")
    public UserResponse updateEmployeeUser(AdminModUserInfoRequest req) {
        return finalizeAdminUpdate(req, AdminManagedUserKind.EMPLOYEE);
    }

    private UserResponse finalizeAdminUpdate(AdminModUserInfoRequest req, AdminManagedUserKind kind) {
        if (req == null || req.getId() == null) {
            throw new NotFoundEntityException(MessageConstant.USER_NOT_FOUND);
        }

        UserEntity actor = getCurrentUserEntity();
        UserEntity targetSnapshot = loadUserForWrite(req.getId());
        assertMatchesAdminKind(targetSnapshot, kind);
        validateRoleChangeForKind(req, kind);

        boolean isTargetDifferentFromActor = isAdmin(actor) && !req.getId().equals(actor.getId());
        String targetUsername = null;
        if (isTargetDifferentFromActor) {
            targetUsername = userRepository.findUsernameById(req.getId())
                    .orElseThrow(() -> new NotFoundEntityException(
                            String.format(MessageConstant.USER_NOT_FOUND_BY_ID, req.getId())));
        }

        UserResponse response = doUpdateUser(req, actor);

        boolean permsTouched = applyPermissionMutationsIfAny(req, actor);
        if (permsTouched) {
            response = toResponse(loadUserForWrite(req.getId()));
            permissionService.clearUserPermissionsCache(response.getUsername());
        }

        permissionService.clearUserPermissionsCache(actor.getUsername());

        if (isTargetDifferentFromActor && targetUsername != null) {
            permissionService.clearUserPermissionsCache(targetUsername);
        }

        log.info("User updated: actorId={}, targetId={}, segment={}", actor.getId(), req.getId(), kind);
        return response;
    }

    /**
     * Đồng bộ cấp/thu hồi quyền cấp thêm trong cùng request cập nhật staff/employee.
     * User không phải {@link RoleConstant#ROLE_SUPER_ADMIN} hoặc {@link RoleConstant#ROLE_ADMIN}
     * không được tự grant/revoke cho chính mình.
     */
    private boolean applyPermissionMutationsIfAny(AdminModUserInfoRequest req, UserEntity actor) {
        boolean hasGrants = req.getGrantPermissionCodes() != null && !req.getGrantPermissionCodes().isEmpty();
        boolean hasRevokes = req.getRevokePermissionCodes() != null && !req.getRevokePermissionCodes().isEmpty();
        if (!hasGrants && !hasRevokes) {
            return false;
        }

        if (req.getId().equals(actor.getId()) && !canSelfDelegatePermissions(actor)) {
            throw new AccessDeniedException(
                    "Chỉ SUPER_ADMIN hoặc ADMIN được tự cấp hoặc thu hồi quyền cho chính mình.");
        }

        if (hasRevokes) {
            rolePermissionService.revokePermissions(RevokePermissionRequest.builder()
                    .userId(req.getId())
                    .permissionCodes(req.getRevokePermissionCodes())
                    .build());
        }
        if (hasGrants) {
            rolePermissionService.grantPermissions(GrantPermissionRequest.builder()
                    .userId(req.getId())
                    .permissionCodes(req.getGrantPermissionCodes())
                    .expiresAt(req.getPermissionGrantExpiresAt())
                    .build());
        }
        return true;
    }

    private static boolean canSelfDelegatePermissions(UserEntity actor) {
        return actor.hasRole(RoleConstant.ROLE_SUPER_ADMIN) || actor.hasRole(RoleConstant.ROLE_ADMIN);
    }

    @Override
    @Transactional
    @CacheEvict(value = { "users", "userPermissions" }, key = "#id")
    public void deleteStaffUser(Long id) {
        UserEntity actor = getCurrentUserEntity();
        if (!PermissionEvaluator.hasAnyPermission(actor.getAllPermissions(),
                PermissionCode.DELETE_USER,
                PermissionCode.DELETE_ALL)) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }
        UserEntity target = loadUserForWrite(id);
        assertMatchesAdminKind(target, AdminManagedUserKind.STAFF);
        if (target.getId().equals(actor.getId())) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Không thể xóa tài khoản của chính bạn.");
        }
        permissionService.clearUserPermissionsCache(target.getUsername());
        userRepository.delete(target);
        log.info("Staff user deleted: id={}, by={}", id, actor.getUsername());
    }

    @Override
    @Transactional
    @CacheEvict(value = { "users", "userPermissions" }, key = "#id")
    public UserResponse resetStaffPassword(Long id) {
        UserEntity actor = getCurrentUserEntity();
        if (!PermissionEvaluator.hasAnyPermission(actor.getAllPermissions(),
                PermissionCode.UPDATE_USER,
                PermissionCode.UPDATE_ALL)) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }
        UserEntity target = loadUserForWrite(id);
        assertMatchesAdminKind(target, AdminManagedUserKind.STAFF);
        String plain = generateStaffInitialPassword(target.getPhoneNumber());
        target.setPassword(passwordEncoder.encode(plain));
        userRepository.save(target);
        permissionService.clearUserPermissionsCache(target.getUsername());
        log.info("Staff password reset: id={}, by={}", id, actor.getUsername());
        return toResponse(target, plain);
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
    public UserResponse createStaffUser(CreateUserRequest request) {
        return createUserForAdminSegment(request, AdminManagedUserKind.STAFF);
    }

    @Override
    @Transactional
    public UserResponse createEmployeeUser(CreateUserRequest request) {
        return createUserForAdminSegment(request, AdminManagedUserKind.EMPLOYEE);
    }

    private UserResponse createUserForAdminSegment(CreateUserRequest request, AdminManagedUserKind kind) {
        if (request == null) {
            throw new IllegalArgumentException("CreateUserRequest cannot be null");
        }

        UserEntity currentUser = getCurrentUserEntity();
        if (!PermissionEvaluator.hasAnyPermission(currentUser.getAllPermissions(),
                PermissionCode.CREATE_USER,
                PermissionCode.CREATE_ALL)) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }

        String username = userValidationHelper.validateAndNormalizeUsername(request.getUsername());
        String phone = userValidationHelper.validateAndNormalizePhone(request.getPhoneNumber());
        boolean generatedPlain = !StringUtils.hasText(request.getPassword());
        String passwordPlain = generatedPlain
                ? generateStaffInitialPassword(phone)
                : userValidationHelper.validatePassword(request.getPassword());

        if (userRepository.existsByUsername(username)) {
            throw new CustomApiException(HttpStatus.CONFLICT, "Username already exists: " + username);
        }
        if (userRepository.existsByPhoneNumber(phone)) {
            throw new CustomApiException(HttpStatus.CONFLICT, "Phone number already exists: " + phone);
        }

        RoleEntity role = kind == AdminManagedUserKind.EMPLOYEE
                ? resolveRoleForEmployeeOnly(request.getRoleId())
                : resolveRoleForCreate(currentUser, request.getRoleId());
        if (kind == AdminManagedUserKind.STAFF
                && RoleConstant.ROLE_CUSTOMER.equals(role.getCode())) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "API nhân viên không tạo tài khoản CUSTOMER; chỉ định role nội bộ.");
        }
        validateHighPrivilegeRolesForPrivilegedCreator(currentUser, role);

        String encodedPassword = passwordEncoder.encode(passwordPlain);
        UserEntity saved = persistNewAdminUser(request, role, currentUser, username, phone, encodedPassword);
        return toResponse(saved, generatedPlain ? passwordPlain : null);
    }

    /**
     * Mật khẩu 6 chữ số: 5 chữ số đầu lấy từ SĐT (bỏ ký tự không phải số), chữ số thứ 6 ngẫu nhiên.
     * Nếu không đủ 5 chữ số từ SĐT, phần đầu được bù bằng chữ số ngẫu nhiên.
     */
    private String generateStaffInitialPassword(String normalizedPhone) {
        String digits = normalizedPhone == null ? "" : normalizedPhone.replaceAll("\\D", "");
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < Math.min(5, digits.length()); i++) {
            prefix.append(digits.charAt(i));
        }
        while (prefix.length() < 5) {
            prefix.append(secureRandom.nextInt(10));
        }
        prefix.append(secureRandom.nextInt(10));
        return prefix.toString();
    }

    private UserEntity persistNewAdminUser(CreateUserRequest request, RoleEntity role, UserEntity currentUser,
                                             String username, String phone, String encodedPassword) {
        UserEntity user = UserEntity.builder()
                .username(username)
                .password(encodedPassword)
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .phoneNumber(phone)
                .status(SystemConstant.ACTIVE_STATUS)
                .build();

        if (isPrivilegedAccountCreator(currentUser)) {
            if (request.getManId() != null) {
                if (!userRepository.existsById(request.getManId())) {
                    throw new CustomApiException(HttpStatus.BAD_REQUEST, "manId không tồn tại: " + request.getManId());
                }
                user.setManId(request.getManId());
            }
        } else {
            user.setManId(currentUser.getId());
        }

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
        user.setRole(role);

        user = userRepository.save(user);

        log.info("User created: id={}, username={}, role={}",
                user.getId(), user.getUsername(), role.getCode());

        return user;
    }

    private void assertMatchesAdminKind(UserEntity user, AdminManagedUserKind kind) {
        if (kind == AdminManagedUserKind.ANY) {
            return;
        }
        if (user.getRole() == null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Tài khoản chưa gán role.");
        }
        String code = user.getRole().getCode();
        if (kind == AdminManagedUserKind.STAFF) {
            if (RoleConstant.ROLE_CUSTOMER.equals(code)) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Tài khoản không thuộc nhóm nhân viên nội bộ.");
            }
        } else if (kind == AdminManagedUserKind.EMPLOYEE) {
            if (!RoleConstant.ROLE_EMPLOYEE.equals(code)) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Tài khoản không phải EMPLOYEE.");
            }
        }
    }

    private void validateRoleChangeForKind(AdminModUserInfoRequest req, AdminManagedUserKind kind) {
        if (req == null || req.getRoleId() == null) {
            return;
        }
        RoleEntity r = roleRepository.findById(req.getRoleId())
                .orElseThrow(() -> new NotFoundEntityException("Role not found: id=" + req.getRoleId()));
        if (kind == AdminManagedUserKind.ANY) {
            validateHighPrivilegeRolesForPrivilegedCreator(getCurrentUserEntity(), r);
            return;
        }
        if (kind == AdminManagedUserKind.STAFF) {
            if (RoleConstant.ROLE_CUSTOMER.equals(r.getCode())) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Không gán CUSTOMER qua API nhân viên.");
            }
        } else if (kind == AdminManagedUserKind.EMPLOYEE) {
            if (!RoleConstant.ROLE_EMPLOYEE.equals(r.getCode())) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "API employees chỉ gán role EMPLOYEE.");
            }
        }
    }

    protected RoleEntity resolveRoleForEmployeeOnly(Long roleId) {
        if (roleId != null) {
            RoleEntity r = roleRepository.findById(roleId)
                    .orElseThrow(() -> new NotFoundEntityException("Role not found: id=" + roleId));
            if (!RoleConstant.ROLE_EMPLOYEE.equals(r.getCode())) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "API employees chỉ gán role EMPLOYEE (không hợp lệ: " + r.getCode() + ")");
            }
            return r;
        }
        return roleRepository.findByCode(RoleConstant.ROLE_EMPLOYEE)
                .orElseThrow(() -> new NotFoundEntityException("Chưa cấu hình role EMPLOYEE trong hệ thống."));
    }

    /**
     * Admin/SUPER_ADMIN: như {@link RoleAssignmentService#assignRoleForPrivilegedCreator(Long)} (mặc định CUSTOMER nếu không gửi roleId).
     * Other: chỉ gán MANAGER / EMPLOYEE, mặc định EMPLOYEE nếu không gửi roleId.
     */
    protected RoleEntity resolveRoleForCreate(UserEntity actor, Long roleId) {
        if (isPrivilegedAccountCreator(actor)) {
            return roleAssignmentService.assignRoleForPrivilegedCreator(roleId);
        }

        if (roleId != null) {
            RoleEntity r = roleRepository.findById(roleId)
                    .orElseThrow(() -> new NotFoundEntityException("Role not found: id=" + roleId));
            String code = r.getCode();
            if (RoleConstant.ROLE_MANAGER.equals(code) || RoleConstant.ROLE_EMPLOYEE.equals(code)) {
                return r;
            }
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Chỉ được gán role MANAGER hoặc EMPLOYEE (không hợp lệ: " + code + ")");
        }
        return roleRepository.findByCode(RoleConstant.ROLE_EMPLOYEE)
                .orElseThrow(() -> new NotFoundEntityException("Chưa cấu hình role EMPLOYEE trong hệ thống."));
    }

    /**
     * Thêm tài khoản ADMIN/SUPER_ADMIN chỉ khi người tạo là ADMIN hoặc SUPER_ADMIN.
     */
    protected void validateHighPrivilegeRolesForPrivilegedCreator(UserEntity actor, RoleEntity role) {
        if (role == null) {
            return;
        }
        Set<String> high = Set.of(RoleConstant.ROLE_ADMIN, RoleConstant.ROLE_SUPER_ADMIN);
        if (!high.contains(role.getCode())) {
            return;
        }
        if (!isPrivilegedAccountCreator(actor)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN,
                    "Chỉ ADMIN hoặc SUPER_ADMIN mới được gán role ADMIN/SUPER_ADMIN.");
        }
    }
}