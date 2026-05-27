package com.ndh.ShopTechnology.services.permission.impl;

import com.ndh.ShopTechnology.constants.MessageConstant;
import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.constants.RoleConstant;
import com.ndh.ShopTechnology.dto.request.permission.GrantPermissionRequest;
import com.ndh.ShopTechnology.dto.request.permission.RevokePermissionRequest;
import com.ndh.ShopTechnology.dto.request.role.UpsertRoleRequest;
import com.ndh.ShopTechnology.dto.response.permission.UserPermissionsResponse;
import com.ndh.ShopTechnology.dto.response.role.RoleResponse;
import com.ndh.ShopTechnology.entities.role.RoleEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.entities.user.UserPermissionEntity;
import com.ndh.ShopTechnology.exception.AuthenticationFailedException;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.RoleRepository;
import com.ndh.ShopTechnology.repository.UserPermissionRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.permission.PermissionEvaluator;
import com.ndh.ShopTechnology.services.permission.PermissionService;
import com.ndh.ShopTechnology.services.permission.RolePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PermissionService permissionService;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        UserEntity actor = currentActor();
        requireAdminOrSuperAdmin(actor);
        return RoleResponse.fromList(roleRepository.findAll());
    }

    @Override
    @Transactional
    public RoleResponse createRole(UpsertRoleRequest request) {
        UserEntity actor = currentActor();
        requireAdminOrSuperAdmin(actor);

        String code = normalizeRoleCode(request.getCode());
        if (roleRepository.existsByCode(code)) {
            throw new CustomApiException(HttpStatus.CONFLICT, "Role code already exists: " + code);
        }

        Set<Integer> defaults = sanitizeAndAuthorizePermissions(actor, request.getPermissionCodes());

        RoleEntity role = RoleEntity.builder()
                .code(code)
                .name(request.getName().trim())
                .description(request.getDescription())
                .status(request.getStatus())
                .permissionCodes(new LinkedHashSet<>(defaults))
                .build();

        RoleEntity saved = roleRepository.save(role);
        permissionService.clearAllPermissionsCache();
        log.info("Role created: code={}, perms={}, by={}", code, defaults, actor.getUsername());
        return RoleResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public RoleResponse updateRole(Long roleId, UpsertRoleRequest request) {
        UserEntity actor = currentActor();
        requireAdminOrSuperAdmin(actor);

        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundEntityException("Role not found: id=" + roleId));

        if (request.getName() != null) role.setName(request.getName().trim());
        if (request.getDescription() != null) role.setDescription(request.getDescription());
        if (request.getStatus() != null) role.setStatus(request.getStatus());

        if (request.getPermissionCodes() != null) {
            Set<Integer> defaults = sanitizeAndAuthorizePermissions(actor, request.getPermissionCodes());
            role.setPermissionCodes(new LinkedHashSet<>(defaults));
        }

        RoleEntity saved = roleRepository.save(role);
        permissionService.clearAllPermissionsCache();
        log.info("Role updated: id={}, code={}, by={}", saved.getId(), saved.getCode(), actor.getUsername());
        return RoleResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        UserEntity actor = currentActor();
        requireAdminOrSuperAdmin(actor);

        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundEntityException("Role not found: id=" + roleId));

        roleRepository.delete(role);
        permissionService.clearAllPermissionsCache();
        log.info("Role deleted: id={}, code={}, by={}", roleId, role.getCode(), actor.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public UserPermissionsResponse getUserPermissions(Long userId) {
        UserEntity actor = currentActor();
        boolean self = actor.getId() != null && actor.getId().equals(userId);
        if (!self && !canViewOtherUsersPermissionData(actor)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, MessageConstant.NO_PERMISSION_ACTION);
        }

        UserEntity user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new NotFoundEntityException(
                        String.format(MessageConstant.USER_NOT_FOUND_BY_ID, userId)));
        return toUserPermissionsResponse(user);
    }

    @Override
    @Transactional
    public UserPermissionsResponse grantPermissions(GrantPermissionRequest request) {
        UserEntity actor = currentActor();
        requireCanDelegateUserPermissions(actor);

        UserEntity target = userRepository.findByIdWithRolesAndPermissions(request.getUserId())
                .orElseThrow(() -> new NotFoundEntityException(
                        String.format(MessageConstant.USER_NOT_FOUND_BY_ID, request.getUserId())));

        Set<Integer> requested = sanitizeAndAuthorizePermissions(actor, request.getPermissionCodes());
        if (requested.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "No valid permission codes provided");
        }

        for (Integer code : requested) {
            if (hasEquivalentPermission(target, code)) {
                continue;
            }

            UserPermissionEntity grant = UserPermissionEntity.builder()
                    .user(target)
                    .permissionCode(code)
                    .assignedBy(actor.getUsername())
                    .expiresAt(request.getExpiresAt())
                    .build();
            userPermissionRepository.save(grant);
        }

        permissionService.clearUserPermissionsCache(target.getUsername());
        log.info("Granted permissions {} to userId={} by {}", requested, target.getId(), actor.getUsername());

        UserEntity refreshed = userRepository.findByIdWithRolesAndPermissions(target.getId())
                .orElseThrow(() -> new NotFoundEntityException(
                        String.format(MessageConstant.USER_NOT_FOUND_BY_ID, target.getId())));
        return toUserPermissionsResponse(refreshed);
    }

    @Override
    @Transactional
    public UserPermissionsResponse revokePermissions(RevokePermissionRequest request) {
        UserEntity actor = currentActor();
        requireCanDelegateUserPermissions(actor);

        UserEntity target = userRepository.findByIdWithRolesAndPermissions(request.getUserId())
                .orElseThrow(() -> new NotFoundEntityException(
                        String.format(MessageConstant.USER_NOT_FOUND_BY_ID, request.getUserId())));

        Set<Integer> codes = sanitizeAndAuthorizePermissions(actor, request.getPermissionCodes());
        if (codes.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "No valid permission codes provided");
        }
        for (Integer code : codes) {
            for (int variant : PermissionCode.expandedMergedCodesEqualToCanonical(code)) {
                userPermissionRepository.deleteByUserIdAndPermissionCode(target.getId(), variant);
            }
        }

        permissionService.clearUserPermissionsCache(target.getUsername());
        log.info("Revoked permissions {} from userId={} by {}", codes, target.getId(), actor.getUsername());

        UserEntity refreshed = userRepository.findByIdWithRolesAndPermissions(target.getId())
                .orElseThrow(() -> new NotFoundEntityException(
                        String.format(MessageConstant.USER_NOT_FOUND_BY_ID, target.getId())));
        return toUserPermissionsResponse(refreshed);
    }

    private void requireCanDelegateUserPermissions(UserEntity actor) {
        if (actor.hasRole(RoleConstant.ROLE_ADMIN) || actor.hasRole(RoleConstant.ROLE_SUPER_ADMIN)) {
            return;
        }
        if (PermissionEvaluator.hasAnyPermission(actor.getAllPermissions(),
                PermissionCode.UPDATE_USER,
                PermissionCode.UPDATE_ALL)) {
            return;
        }
        throw new CustomApiException(HttpStatus.FORBIDDEN, MessageConstant.NO_PERMISSION_ACTION);
    }

    private boolean canViewOtherUsersPermissionData(UserEntity actor) {
        if (actor.hasRole(RoleConstant.ROLE_ADMIN) || actor.hasRole(RoleConstant.ROLE_SUPER_ADMIN)) {
            return true;
        }
        return PermissionEvaluator.hasAnyPermission(actor.getAllPermissions(),
                PermissionCode.READ_USER,
                PermissionCode.READ_ALL,
                PermissionCode.UPDATE_USER,
                PermissionCode.UPDATE_ALL);
    }

    private UserEntity currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationFailedException(MessageConstant.AUTH_FAILED);
        }
        return userRepository.findByUsernameWithRolesAndPermissions(auth.getName())
                .orElseThrow(() -> new AuthenticationFailedException(MessageConstant.AUTH_FAILED));
    }

    private void requireActorPermission(int... codes) {
        UserEntity actor = currentActor();
        requireActorPermission(actor, codes);
    }

    private void requireActorPermission(UserEntity actor, int... codes) {
        Set<Integer> effective = actor.getAllPermissions();
        if (!PermissionEvaluator.hasAnyPermission(effective, codes)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, MessageConstant.NO_PERMISSION_ACTION);
        }
    }

    private void requireAdminOrSuperAdmin(UserEntity actor) {
        if (!actor.hasRole(RoleConstant.ROLE_ADMIN) && !actor.hasRole(RoleConstant.ROLE_SUPER_ADMIN)) {
            throw new CustomApiException(HttpStatus.FORBIDDEN, MessageConstant.NO_PERMISSION_ACTION);
        }
    }

    private Set<Integer> sanitizeAndAuthorizePermissions(UserEntity actor, Collection<Integer> rawCodes) {
        Set<Integer> cleaned = sanitizeCodes(rawCodes);
        if (cleaned.isEmpty()) return cleaned;

        Set<Integer> codes = normalizedPermissionCodes(cleaned);

        Set<Integer> known = PermissionCode.allKnownCodes();
        Set<Integer> unknown = cleaned.stream().filter(c -> !known.contains(c)).collect(Collectors.toCollection(LinkedHashSet::new));
        if (!unknown.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Unknown permission codes: " + unknown);
        }

        Set<Integer> actorEffective = actor.getAllPermissions();
        Set<Integer> notAllowed = codes.stream()
                .filter(c -> !PermissionEvaluator.hasPermission(actorEffective, c))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!notAllowed.isEmpty()) {
            throw new CustomApiException(HttpStatus.FORBIDDEN,
                    "You cannot grant permissions you do not have: " + notAllowed);
        }
        return codes;
    }

    private Set<Integer> sanitizeCodes(Collection<Integer> raw) {
        if (raw == null || raw.isEmpty()) return new LinkedHashSet<>();
        return raw.stream().filter(c -> c != null && c > 0).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<Integer> normalizedPermissionCodes(Collection<Integer> cleaned) {
        return cleaned.stream()
                .map(PermissionCode::normalizeGrantPermissionCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean overlapsMerged(Set<Integer> pool, int canonPermission) {
        for (int v : PermissionCode.expandedMergedCodesEqualToCanonical(canonPermission)) {
            if (pool.contains(v)) return true;
        }
        return false;
    }

    private boolean hasEquivalentPermission(UserEntity target, int canonPermission) {
        if (overlapsMerged(rolePermissionsOf(target), canonPermission)) {
            return true;
        }
        if (target.getUserPermissions() == null) return false;
        LocalDateTime now = LocalDateTime.now();
        for (UserPermissionEntity up : target.getUserPermissions()) {
            if (up.getPermissionCode() == null) continue;
            if (up.getExpiresAt() != null && up.getExpiresAt().isBefore(now)) continue;
            if (PermissionCode.normalizeGrantPermissionCode(up.getPermissionCode()) == canonPermission) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeRoleCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private Set<Integer> rolePermissionsOf(UserEntity user) {
        Set<Integer> rolePerms = new HashSet<>();
        if (user.getRole() != null && user.getRole().getPermissionCodes() != null) {
            rolePerms.addAll(user.getRole().getPermissionCodes());
        }
        return rolePerms;
    }

    private UserPermissionsResponse toUserPermissionsResponse(UserEntity user) {
        Set<Integer> rolePerms = rolePermissionsOf(user);

        Set<Integer> userPerms = new LinkedHashSet<>();
        if (user.getUserPermissions() != null) {
            LocalDateTime now = LocalDateTime.now();
            for (UserPermissionEntity up : user.getUserPermissions()) {
                if (up.getPermissionCode() == null) continue;
                if (up.getExpiresAt() != null && up.getExpiresAt().isBefore(now)) continue;
                userPerms.add(up.getPermissionCode());
            }
        }

        Set<Integer> effective = new LinkedHashSet<>();
        effective.addAll(rolePerms);
        effective.addAll(userPerms);

        Set<String> roleCodes = new LinkedHashSet<>();
        if (user.getRole() != null) {
            roleCodes.add(user.getRole().getCode());
        }

        return UserPermissionsResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .roles(roleCodes)
                .rolePermissions(rolePerms)
                .userPermissions(userPerms)
                .effectivePermissions(effective)
                .build();
    }
}
