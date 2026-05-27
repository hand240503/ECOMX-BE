package com.ndh.ShopTechnology.services.permission;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRepository userRepository;

    @Cacheable(value = "userPermissions", key = "#username")
    @Transactional(readOnly = true)
    public Set<Integer> getUserPermissions(String username) {
        log.debug("Loading permissions for user: {}", username);

        UserEntity user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return user.getAllPermissions();
    }

    public boolean hasPermission(String username, int code) {
        Set<Integer> permissions = getUserPermissions(username);
        return PermissionEvaluator.hasPermission(permissions, code);
    }

    public boolean hasAnyPermission(String username, int... codes) {
        if (codes == null || codes.length == 0) return false;
        Set<Integer> permissions = getUserPermissions(username);
        return PermissionEvaluator.hasAnyPermission(permissions, codes);
    }

    public boolean hasAllPermissions(String username, int... codes) {
        if (codes == null || codes.length == 0) return false;
        Set<Integer> permissions = getUserPermissions(username);
        return PermissionEvaluator.hasAllPermissions(permissions, codes);
    }

    public void requireAnyPermission(int... codes) {
        String username = currentUsername();
        if (username == null) {
            throw new AccessDeniedException("Yêu cầu đăng nhập để thực hiện thao tác này");
        }
        if (codes == null || codes.length == 0) {
            return;
        }
        if (!hasAnyPermission(username, codes)) {
            throw new AccessDeniedException(
                    "Tài khoản không có quyền (yêu cầu 1 trong các quyền: "
                            + Arrays.toString(codes) + ")");
        }
    }

    public void requirePermission(int code) {
        String username = currentUsername();
        if (username == null) {
            throw new AccessDeniedException("Yêu cầu đăng nhập để thực hiện thao tác này");
        }
        if (!hasPermission(username, code)) {
            throw new AccessDeniedException(
                    "Tài khoản không có quyền (yêu cầu quyền: " + code + ")");
        }
    }

    public void requireAllPermissions(int... codes) {
        String username = currentUsername();
        if (username == null) {
            throw new AccessDeniedException("Yêu cầu đăng nhập để thực hiện thao tác này");
        }
        if (codes == null || codes.length == 0) {
            return;
        }
        if (!hasAllPermissions(username, codes)) {
            throw new AccessDeniedException(
                    "Tài khoản không đủ quyền (yêu cầu tất cả các quyền: "
                            + Arrays.toString(codes) + ")");
        }
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        String name = auth.getName();
        return (name == null || name.isBlank()) ? null : name;
    }

    @CacheEvict(value = "userPermissions", key = "#username")
    public void clearUserPermissionsCache(String username) {
        log.info("Cleared permissions cache for user: {}", username);
    }

    @CacheEvict(value = "userPermissions", allEntries = true)
    public void clearAllPermissionsCache() {
        log.info("Cleared all permissions cache");
    }
}
