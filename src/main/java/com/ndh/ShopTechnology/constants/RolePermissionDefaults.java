package com.ndh.ShopTechnology.constants;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.ndh.ShopTechnology.constants.PermissionCode.*;

public final class RolePermissionDefaults {

    private RolePermissionDefaults() {
    }

    public static final Set<Integer> SUPER_ADMIN_PERMISSIONS = unmodifiableSetOf(
            CREATE_ALL, READ_ALL, UPDATE_ALL, DELETE_ALL
    );

    public static final Set<Integer> ADMIN_PERMISSIONS = unmodifiableSetOf(
            CREATE_ALL, READ_ALL, UPDATE_ALL, DELETE_ALL
    );

    public static final Set<Integer> MANAGER_PERMISSIONS = unmodifiableSetOf(
            CREATE_ALL, READ_ALL, UPDATE_ALL
    );

    public static final Set<Integer> EMPLOYEE_PERMISSIONS = unmodifiableSetOf(
            READ_PRODUCT
    );

    public static final Set<Integer> CUSTOMER_PERMISSIONS = unmodifiableSetOf(
            READ_PRODUCT
    );

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
