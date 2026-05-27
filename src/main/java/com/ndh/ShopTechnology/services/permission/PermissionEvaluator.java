package com.ndh.ShopTechnology.services.permission;

import com.ndh.ShopTechnology.constants.PermissionCode;

import java.util.Set;

public final class PermissionEvaluator {

    private PermissionEvaluator() {
    }

    public static boolean hasPermission(Set<Integer> effective, int required) {
        if (effective == null || effective.isEmpty()) return false;
        if (effective.contains(required)) return true;

        if (PermissionCode.isModuleSpecific(required)) {
            int sysWide = PermissionCode.systemWideForAction(required);
            if (sysWide > 0 && effective.contains(sysWide)) return true;

            int canonReq = PermissionCode.normalizeGrantPermissionCode(required);
            for (Integer granted : effective) {
                if (granted != null && PermissionCode.isModuleSpecific(granted)
                        && PermissionCode.normalizeGrantPermissionCode(granted) == canonReq) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasAnyPermission(Set<Integer> effective, int... required) {
        if (required == null || required.length == 0) return false;
        for (int code : required) {
            if (hasPermission(effective, code)) return true;
        }
        return false;
    }

    public static boolean hasAllPermissions(Set<Integer> effective, int... required) {
        if (required == null || required.length == 0) return false;
        for (int code : required) {
            if (!hasPermission(effective, code)) return false;
        }
        return true;
    }
}
