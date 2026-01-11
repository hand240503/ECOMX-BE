package com.ndh.ShopTechnology.services.permission;

import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRepository userRepository;

    /**
     * Lấy tất cả permissions của user (có cache)
     */
    @Cacheable(value = "userPermissions", key = "#username")
    @Transactional(readOnly = true)
    public Set<String> getUserPermissions(String username) {
        log.debug("Loading permissions for user: {}", username);

        UserEntity user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return user.getAllPermissions();
    }

    /**
     * Kiểm tra user có quyền
     */
    public boolean hasPermission(String username, String permission) {
        Set<String> permissions = getUserPermissions(username);
        return permissions.contains(permission) || permissions.contains("admin:all");
    }

    /**
     * Kiểm tra user có ít nhất 1 quyền
     */
    public boolean hasAnyPermission(String username, String... permissions) {
        if (permissions == null || permissions.length == 0) return true;

        Set<String> userPerms = getUserPermissions(username);

        // Admin có tất cả quyền
        if (userPerms.contains("admin:all")) return true;

        for (String perm : permissions) {
            if (userPerms.contains(perm)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Kiểm tra user có tất cả quyền
     */
    public boolean hasAllPermissions(String username, String... permissions) {
        if (permissions == null || permissions.length == 0) return true;

        Set<String> userPerms = getUserPermissions(username);

        // Admin có tất cả quyền
        if (userPerms.contains("admin:all")) return true;

        for (String perm : permissions) {
            if (!userPerms.contains(perm)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Clear cache khi thay đổi quyền
     */
    @CacheEvict(value = "userPermissions", key = "#username")
    public void clearUserPermissionsCache(String username) {
        log.info("Cleared permissions cache for user: {}", username);
    }

    /**
     * Clear toàn bộ cache
     */
    @CacheEvict(value = "userPermissions", allEntries = true)
    public void clearAllPermissionsCache() {
        log.info("Cleared all permissions cache");
    }
}