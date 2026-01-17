package com.ndh.ShopTechnology.services.user.impl;

import com.ndh.ShopTechnology.constant.MessageConstant;
import com.ndh.ShopTechnology.constant.SystemConstant;
import com.ndh.ShopTechnology.dto.request.PaginationRequest;
import com.ndh.ShopTechnology.dto.request.user.CreateUserRequest;
import com.ndh.ShopTechnology.dto.request.user.ModUserInfoRequest;

import com.ndh.ShopTechnology.dto.response.user.UserResponse;
import com.ndh.ShopTechnology.entities.role.RoleEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.entities.user.UserInfoEntity;
import com.ndh.ShopTechnology.exception.AuthenticationFailedException;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.RoleRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.permission.PermissionService;
import com.ndh.ShopTechnology.services.user.UserService;
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
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

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

    @Cacheable(value = "users", key = "#id")
    @Transactional(readOnly = true)
    public UserEntity loadUser(Long id) {
        if (id == null) {
            return getCurrentUserEntity();
        }

        return userRepository.findById(id)
                .map(user -> {
                    // Trigger lazy loading for roles and permissions
                    user.getRoles().size();
                    user.getUserPermissions().size();
                    if (user.getUserInfo() != null) {
                        user.getUserInfo().getFirstName();
                    }
                    return user;
                })
                .orElseThrow(() -> new NotFoundEntityException(
                        String.format(MessageConstant.USER_NOT_FOUND_BY_ID, id)));
    }

    protected UserResponse toResponse(UserEntity user) {
        if (user == null)
            return null;
        return UserResponse.fromEntity(user);
    }

    protected void applyUpdateToUser(UserEntity user, ModUserInfoRequest req) {
        if (req == null || user == null)
            return;

        // Update core user fields
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

        // Update or create UserInfo
        UserInfoEntity userInfo = user.getUserInfo();
        if (userInfo == null) {
            userInfo = new UserInfoEntity();
            userInfo.setUser(user);
            user.setUserInfo(userInfo);
        }

        if (req.getFirstName() != null)
            userInfo.setFirstName(req.getFirstName());
        if (req.getLastName() != null)
            userInfo.setLastName(req.getLastName());
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

    /* ================== Services ================== */

    @Override
    public Page<UserResponse> getAllUsers(PaginationRequest request) {
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
    public UserResponse updateUserInfo(ModUserInfoRequest req) {
        if (req == null || req.getId() == null) {
            throw new NotFoundEntityException(MessageConstant.USER_NOT_FOUND);
        }

        UserEntity currentUser = getCurrentUser();

        if (!currentUser.hasAnyPermission("user:update", "admin:all")) {
            throw new AccessDeniedException(MessageConstant.NO_PERMISSION_ACTION);
        }

        UserEntity target = loadUser(req.getId());

        UserResponse response = doUpdateUserInfo(req, target);

        permissionService.clearUserPermissionsCache(target.getUsername());

        log.info("User updated: id={}, username={}", target.getId(), target.getUsername());

        return response;
    }

    @Override
    @Transactional
    public UserResponse updateProfileInfo(ModUserInfoRequest req) {
        UserEntity currentUser = getCurrentUserEntity();

        UserResponse response = doUpdateUserInfo(req, currentUser);

        permissionService.clearUserPermissionsCache(currentUser.getUsername());

        log.info("Profile updated: username={}", currentUser.getUsername());

        return response;
    }

    @Transactional
    protected UserResponse doUpdateUserInfo(ModUserInfoRequest req, UserEntity user) {
        if (user == null) {
            throw new NotFoundEntityException(MessageConstant.USER_NOT_FOUND);
        }

        applyUpdateToUser(user, req);

        if (req.getRoleIds() != null && !req.getRoleIds().isEmpty()) {
            Set<RoleEntity> roles = new HashSet<>();
            for (Long roleId : req.getRoleIds()) {
                roleRepository.findById(roleId).ifPresent(roles::add);
            }

            if (!roles.isEmpty()) {
                user.setRoles(roles);
            } else {
                log.warn("No valid roles found for roleIds: {}", req.getRoleIds());
            }
        }

        user = userRepository.save(user);

        return toResponse(user);
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

        String username = validateAndNormalizeUsername(request.getUsername());
        String phone = validateAndNormalizePhone(request.getPhoneNumber());
        String password = validatePassword(request.getPassword());

        if (userRepository.existsByUsername(username)) {
            throw new CustomApiException(HttpStatus.CONFLICT, "Username already exists: " + username);
        }
        if (userRepository.existsByPhoneNumber(phone)) {
            throw new CustomApiException(HttpStatus.CONFLICT, "Phone number already exists: " + phone);
        }

        // Build UserEntity
        UserEntity user = UserEntity.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .phoneNumber(phone)
                .status(SystemConstant.ACTIVE_STATUS)
                .build();

        // Build UserInfoEntity
        UserInfoEntity userInfo = UserInfoEntity.builder()
                .user(user)
                .firstName(request.getFirstName() != null ? request.getFirstName().trim() : null)
                .lastName(request.getLastName() != null ? request.getLastName().trim() : null)
                .avatar(request.getAvatar())
                .managerId(request.getManagerId() != null ? request.getManagerId() : currentUser.getId())
                .info01(request.getInfo01())
                .info02(request.getInfo02())
                .info03(request.getInfo03())
                .info04(request.getInfo04())
                .build();

        user.setUserInfo(userInfo);

        Set<RoleEntity> roles = assignRoles(request.getRoleIds());
        user.setRoles(roles);

        user = userRepository.save(user);

        log.info("User created: id={}, username={}, roles={}",
                user.getId(), user.getUsername(), roles.stream().map(RoleEntity::getCode).toList());

        return toResponse(user);
    }

    protected String validateAndNormalizeUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        return username.toLowerCase().trim();
    }

    protected String validateAndNormalizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        return phone.trim();
    }

    protected String validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        return password.trim();
    }

    protected Set<RoleEntity> assignRoles(Set<Long> roleIds) {
        Set<RoleEntity> roles = new HashSet<>();

        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                roleRepository.findById(roleId).ifPresent(roles::add);
            }
        }

        if (roles.isEmpty()) {
            roleRepository.findByCode("ROLE_USER").ifPresent(roles::add);
        }

        if (roles.isEmpty()) {
            throw new NotFoundEntityException("No valid roles found. Please check role configuration.");
        }

        return roles;
    }
}