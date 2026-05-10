package com.ndh.ShopTechnology.services.permission;

import com.ndh.ShopTechnology.constants.PermissionCode;

import java.util.Set;

/**
 * Tiện ích kiểm tra quyền hiệu lực (effective permission) theo quy tắc:
 * <ul>
 *   <li>Quyền hiệu lực = (permission của role) ∪ (permission cấp thêm cho user).</li>
 *   <li>Mã 3 chữ số (101..104) "bao trùm" mọi mã 6 chữ số tương ứng cùng action.</li>
 * </ul>
 *
 * <p>Ví dụ: user có quyền {@code 102} (Read all) → tự động qua mọi check {@code 100002, 200002, 300002,...}.
 *
 * <p>Đây là class thuần tĩnh (no Spring) — gọi được từ entity, service hoặc test.
 */
public final class PermissionEvaluator {

    private PermissionEvaluator() {
    }

    /**
     * Trả về true nếu {@code effective} chứa quyền tương đương {@code required} (xét cả wildcard 101..104).
     */
    public static boolean hasPermission(Set<Integer> effective, int required) {
        if (effective == null || effective.isEmpty()) return false;
        if (effective.contains(required)) return true;

        if (PermissionCode.isModuleSpecific(required)) {
            int sysWide = PermissionCode.systemWideForAction(required);
            return sysWide > 0 && effective.contains(sysWide);
        }
        return false;
    }

    /** True nếu user có ít nhất 1 trong các quyền yêu cầu. */
    public static boolean hasAnyPermission(Set<Integer> effective, int... required) {
        if (required == null || required.length == 0) return false;
        for (int code : required) {
            if (hasPermission(effective, code)) return true;
        }
        return false;
    }

    /** True nếu user có TẤT CẢ các quyền yêu cầu. */
    public static boolean hasAllPermissions(Set<Integer> effective, int... required) {
        if (required == null || required.length == 0) return false;
        for (int code : required) {
            if (!hasPermission(effective, code)) return false;
        }
        return true;
    }
}
