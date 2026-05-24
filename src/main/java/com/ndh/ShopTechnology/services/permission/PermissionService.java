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

/**
 * Service đọc quyền hiệu lực của user (có cache theo username).
 *
 * <p>Toàn bộ kiểm tra wildcard (101..104) và gộp nhánh quyền được uỷ quyền cho {@link PermissionEvaluator}.
 *
 * <p>Trong controller/service, thay vì biến SpEL phức tạp, gọi
 * {@link #requireAnyPermission(int...)} / {@link #requirePermission(int)} trong thân method.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRepository userRepository;

    /**
     * Lấy tất cả permission code (đã hợp role + cấp thêm) của user. Có cache.
     */
    @Cacheable(value = "userPermissions", key = "#username")
    @Transactional(readOnly = true)
    public Set<Integer> getUserPermissions(String username) {
        log.debug("Loading permissions for user: {}", username);

        UserEntity user = userRepository.findByUsernameWithRolesAndPermissions(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return user.getAllPermissions();
    }

    /**
     * Kiểm tra user có 1 quyền cụ thể.
     */
    public boolean hasPermission(String username, int code) {
        Set<Integer> permissions = getUserPermissions(username);
        return PermissionEvaluator.hasPermission(permissions, code);
    }

    /**
     * Kiểm tra user có ít nhất 1 trong các quyền.
     */
    public boolean hasAnyPermission(String username, int... codes) {
        if (codes == null || codes.length == 0) return false;
        Set<Integer> permissions = getUserPermissions(username);
        return PermissionEvaluator.hasAnyPermission(permissions, codes);
    }

    /**
     * Kiểm tra user có TẤT CẢ các quyền.
     */
    public boolean hasAllPermissions(String username, int... codes) {
        if (codes == null || codes.length == 0) return false;
        Set<Integer> permissions = getUserPermissions(username);
        return PermissionEvaluator.hasAllPermissions(permissions, codes);
    }

    /**
     * Đảm bảo user hiện tại có ÍT NHẤT 1 trong các quyền {@code codes}, ngược lại ném
     * {@link AccessDeniedException} (Spring map HTTP 403 qua handler toàn cục nếu có).
     *
     * @param codes danh sách permission code yêu cầu (vd {@link PermissionCode#READ_USER})
     */
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

    /**
     * Đảm bảo user hiện tại có 1 quyền cụ thể (xét cả wildcard system-wide và gộp nhánh).
     */
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

    /**
     * Đảm bảo user hiện tại có TẤT CẢ các quyền chỉ định.
     */
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
