package com.ndh.ShopTechnology.constants;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.ndh.ShopTechnology.constants.PermissionCode.*;

/**
 * Permission mặc định cho từng role. Được nạp lúc khởi động qua {@code RolePermissionBootstrapper}.
 *
 * <p>Các giá trị ở đây phản ánh đúng yêu cầu nghiệp vụ:
 * <ul>
 *   <li><b>SUPER_ADMIN</b>: toàn quyền + khóa user, quản lý role, cấp quyền.</li>
 *   <li><b>ADMIN</b>: toàn quyền CRUD + cấp quyền cho user.</li>
 *   <li><b>MANAGER</b>: Create + Read + Update mọi module (không Delete) + cấp quyền.</li>
 *   <li><b>EMPLOYEE</b>: Read Product + Read Category (mặc định, có thể được cấp thêm).</li>
 *   <li><b>CUSTOMER</b>: chỉ Read Product (xem catalog) — không được cấp quyền.</li>
 * </ul>
 */
public final class RolePermissionDefaults {

    private RolePermissionDefaults() {
    }

    public static final Set<Integer> SUPER_ADMIN_PERMISSIONS = unmodifiableSetOf(
            CREATE_ALL, READ_ALL, UPDATE_ALL, DELETE_ALL,
            LOCK_USER, MANAGE_ROLE, GRANT_PERMISSION
    );

    public static final Set<Integer> ADMIN_PERMISSIONS = unmodifiableSetOf(
            CREATE_ALL, READ_ALL, UPDATE_ALL, DELETE_ALL,
            GRANT_PERMISSION
    );

    public static final Set<Integer> MANAGER_PERMISSIONS = unmodifiableSetOf(
            CREATE_ALL, READ_ALL, UPDATE_ALL,
            GRANT_PERMISSION
    );

    public static final Set<Integer> EMPLOYEE_PERMISSIONS = unmodifiableSetOf(
            READ_PRODUCT, READ_PRICE, READ_UNIT, READ_BRAND, READ_CATEGORY
    );

    public static final Set<Integer> CUSTOMER_PERMISSIONS = unmodifiableSetOf(
            READ_PRODUCT
    );

    /** Mapping role code → permission mặc định (giữ thứ tự để dễ đọc log). */
    public static final Map<String, Set<Integer>> DEFAULTS;
    static {
        Map<String, Set<Integer>> m = new LinkedHashMap<>();
        m.put(RoleConstant.ROLE_SUPER_ADMIN, SUPER_ADMIN_PERMISSIONS);
        m.put(RoleConstant.ROLE_ADMIN,       ADMIN_PERMISSIONS);
        m.put(RoleConstant.ROLE_MANAGER,     MANAGER_PERMISSIONS);
        m.put(RoleConstant.ROLE_EMPLOYEE,    EMPLOYEE_PERMISSIONS);
        m.put(RoleConstant.ROLE_CUSTOMER,    CUSTOMER_PERMISSIONS);
        DEFAULTS = Collections.unmodifiableMap(m);
    }

    private static Set<Integer> unmodifiableSetOf(Integer... values) {
        Set<Integer> s = new LinkedHashSet<>();
        Collections.addAll(s, values);
        return Collections.unmodifiableSet(s);
    }
}
