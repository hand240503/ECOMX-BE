package com.ndh.ShopTechnology.constants;

/**
 * Role codes của hệ thống. Dùng đồng thời:
 * <ul>
 *     <li>Trong {@code WebSecurityConfig} (qua {@code hasAnyRole(...)} → Spring Security tự thêm prefix {@code ROLE_}).</li>
 *     <li>Trong {@code RolePermissionBootstrapper} để seed dữ liệu mặc định.</li>
 * </ul>
 */
public final class RoleConstant {

    private RoleConstant() {
    }

    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ADMIN       = "ADMIN";
    public static final String ROLE_MANAGER     = "MANAGER";
    public static final String ROLE_EMPLOYEE    = "EMPLOYEE";
    public static final String ROLE_CUSTOMER    = "CUSTOMER";

    // Backward-compat (một số chỗ legacy dùng "USER"). Map về CUSTOMER ở RoleAssignmentService.
    public static final String ROLE_USER        = "USER";
}
