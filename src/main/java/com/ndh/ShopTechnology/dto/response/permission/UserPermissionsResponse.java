package com.ndh.ShopTechnology.dto.response.permission;

import lombok.*;

import java.util.Set;

/**
 * Tổng hợp quyền của 1 user (role-default + cấp thêm + tổng hiệu lực).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPermissionsResponse {

    private Long userId;
    private String username;

    /** Tập role code (vd ADMIN, EMPLOYEE). */
    private Set<String> roles;

    /** Permission đến từ role mặc định. */
    private Set<Integer> rolePermissions;

    /** Permission cấp thêm cho user (qua bảng user_permission_grants). */
    private Set<Integer> userPermissions;

    /** Quyền hiệu lực = role + user (chưa bao gồm wildcard). */
    private Set<Integer> effectivePermissions;
}
