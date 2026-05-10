package com.ndh.ShopTechnology.services.permission;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Bean được expose dưới tên {@code @perm} để dùng trong SpEL của {@code @PreAuthorize}:
 *
 * <pre>
 *     // Yêu cầu quyền tạo Product (100001) hoặc system-wide CREATE_ALL (101)
 *     &#64;PreAuthorize("@perm.check(100001)")
 *     public ResponseEntity&lt;...&gt; createProduct(...) { ... }
 * </pre>
 *
 * <p><b>Lưu ý:</b> chỉ giữ {@link #check(int)} cho check 1 quyền. Với "check any" / "check all"
 * thì gọi trực tiếp {@code permissionService.requireAnyPermission(...) /
 * requireAllPermissions(...)} ngay trong thân method (xem {@link PermissionService}). Không dùng
 * SpEL với mảng vararg vì style imperative dễ đọc và debug hơn.
 */
@Component("perm")
@RequiredArgsConstructor
public class PermissionAuthorizationBean {

    private final PermissionService permissionService;

    public boolean check(int code) {
        String username = currentUsername();
        if (username == null) return false;
        return permissionService.hasPermission(username, code);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        String name = auth.getName();
        return (name == null || name.isBlank()) ? null : name;
    }
}
