package com.ndh.ShopTechnology.constants;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PermissionCode {

    private PermissionCode() {
    }

    public static final int CREATE_ALL = 101;
    public static final int READ_ALL   = 102;
    public static final int UPDATE_ALL = 103;
    public static final int DELETE_ALL = 104;
    public static final int LOCK_USER  = 110;
    public static final int MANAGE_ROLE = 111;

    public static final int MODULE_PRODUCT    = 100;
    public static final int MODULE_PRICE      = 150;
    public static final int MODULE_UNIT       = 160;
    public static final int MODULE_BRAND      = 170;
    public static final int MODULE_CATEGORY   = 200;
    public static final int MODULE_DOCUMENT   = 300;
    public static final int MODULE_EMPLOYEE   = 400;
    public static final int MODULE_ORDER      = 500;
    public static final int MODULE_REPORT     = 600;
    public static final int MODULE_USER       = 700;
    public static final int MODULE_ROLE       = 800;
    public static final int MODULE_PERMISSION = 900;

    private static final int[] PRODUCT_MERGED_SOURCE_MODULES =
            new int[]{MODULE_PRODUCT, MODULE_PRICE, MODULE_UNIT, MODULE_BRAND, MODULE_CATEGORY};
    private static final int[] USER_MERGED_SOURCE_MODULES = new int[]{MODULE_USER, MODULE_EMPLOYEE};

    public static final int ACTION_CREATE = 1;
    public static final int ACTION_READ   = 2;
    public static final int ACTION_UPDATE = 3;
    public static final int ACTION_DELETE = 4;

    public static final int CREATE_PRODUCT = 100001;
    public static final int READ_PRODUCT   = 100002;
    public static final int UPDATE_PRODUCT = 100003;
    public static final int DELETE_PRODUCT = 100004;

    public static final int CREATE_PRICE = 150001;
    public static final int READ_PRICE   = 150002;
    public static final int UPDATE_PRICE = 150003;
    public static final int DELETE_PRICE = 150004;

    public static final int CREATE_UNIT = 160001;
    public static final int READ_UNIT   = 160002;
    public static final int UPDATE_UNIT = 160003;
    public static final int DELETE_UNIT = 160004;

    public static final int CREATE_BRAND = 170001;
    public static final int READ_BRAND   = 170002;
    public static final int UPDATE_BRAND = 170003;
    public static final int DELETE_BRAND = 170004;

    public static final int CREATE_CATEGORY = 200001;
    public static final int READ_CATEGORY   = 200002;
    public static final int UPDATE_CATEGORY = 200003;
    public static final int DELETE_CATEGORY = 200004;

    public static final int CREATE_DOCUMENT = 300001;
    public static final int READ_DOCUMENT   = 300002;
    public static final int UPDATE_DOCUMENT = 300003;
    public static final int DELETE_DOCUMENT = 300004;

    public static final int CREATE_EMPLOYEE = 400001;
    public static final int READ_EMPLOYEE   = 400002;
    public static final int UPDATE_EMPLOYEE = 400003;
    public static final int DELETE_EMPLOYEE = 400004;

    public static final int CREATE_ORDER = 500001;
    public static final int READ_ORDER   = 500002;
    public static final int UPDATE_ORDER = 500003;
    public static final int DELETE_ORDER = 500004;

    public static final int CREATE_REPORT = 600001;
    public static final int READ_REPORT   = 600002;
    public static final int UPDATE_REPORT = 600003;
    public static final int DELETE_REPORT = 600004;

    public static final int CREATE_USER = 700001;
    public static final int READ_USER   = 700002;
    public static final int UPDATE_USER = 700003;
    public static final int DELETE_USER = 700004;

    public static int moduleAction(int moduleCode, int actionCode) {
        if (moduleCode < 100 || moduleCode > 999) {
            throw new IllegalArgumentException("moduleCode must be 3 digits: " + moduleCode);
        }
        if (actionCode < 1 || actionCode > 999) {
            throw new IllegalArgumentException("actionCode must be 1..999: " + actionCode);
        }
        return moduleCode * 1000 + actionCode;
    }

    public static boolean isSystemWide(int code) {
        return code >= 100 && code <= 999;
    }

    public static boolean isModuleSpecific(int code) {
        return code >= 100_000 && code <= 999_999;
    }

    public static int extractAction(int code) {
        if (!isModuleSpecific(code)) return -1;
        return code % 1000;
    }

    public static int extractModule(int code) {
        if (!isModuleSpecific(code)) return -1;
        return code / 1000;
    }

    public static int systemWideForAction(int code) {
        int action = extractAction(code);
        switch (action) {
            case ACTION_CREATE: return CREATE_ALL;
            case ACTION_READ:   return READ_ALL;
            case ACTION_UPDATE: return UPDATE_ALL;
            case ACTION_DELETE: return DELETE_ALL;
            default: return -1;
        }
    }

    public static int normalizeGrantPermissionCode(int code) {
        if (!isModuleSpecific(code)) {
            return code;
        }
        int m = extractModule(code);
        int a = extractAction(code);
        for (int merged : PRODUCT_MERGED_SOURCE_MODULES) {
            if (m == merged) {
                return moduleAction(MODULE_PRODUCT, a);
            }
        }
        for (int merged : USER_MERGED_SOURCE_MODULES) {
            if (m == merged) {
                return moduleAction(MODULE_USER, a);
            }
        }
        return code;
    }

    public static LinkedHashSet<Integer> expandedMergedCodesEqualToCanonical(int canonOrLegacy) {
        int canon = normalizeGrantPermissionCode(canonOrLegacy);
        LinkedHashSet<Integer> out = new LinkedHashSet<>();
        if (!isModuleSpecific(canon)) {
            out.add(canon);
            return out;
        }
        int module = extractModule(canon);
        int action = extractAction(canon);
        if (module == MODULE_PRODUCT) {
            for (int merged : PRODUCT_MERGED_SOURCE_MODULES) {
                out.add(moduleAction(merged, action));
            }
        } else if (module == MODULE_USER) {
            for (int merged : USER_MERGED_SOURCE_MODULES) {
                out.add(moduleAction(merged, action));
            }
        } else {
            out.add(canon);
        }
        return out;
    }

    public static List<Integer> catalogGrantableModuleSpecificCodesOrdered() {
        List<Integer> out = new ArrayList<>(20);
        int[] modules = new int[]{MODULE_PRODUCT, MODULE_DOCUMENT, MODULE_ORDER, MODULE_REPORT, MODULE_USER};
        for (int module : modules) {
            for (int action = ACTION_CREATE; action <= ACTION_DELETE; action++) {
                out.add(moduleAction(module, action));
            }
        }
        return out;
    }

    public static Set<Integer> allKnownCodes() {
        return KNOWN_CODES;
    }

    private static final Set<Integer> KNOWN_CODES;
    static {
        Set<Integer> s = new LinkedHashSet<>(Arrays.asList(
                CREATE_ALL, READ_ALL, UPDATE_ALL, DELETE_ALL, LOCK_USER, MANAGE_ROLE,
                CREATE_PRODUCT, READ_PRODUCT, UPDATE_PRODUCT, DELETE_PRODUCT,
                CREATE_PRICE, READ_PRICE, UPDATE_PRICE, DELETE_PRICE,
                CREATE_UNIT, READ_UNIT, UPDATE_UNIT, DELETE_UNIT,
                CREATE_BRAND, READ_BRAND, UPDATE_BRAND, DELETE_BRAND,
                CREATE_CATEGORY, READ_CATEGORY, UPDATE_CATEGORY, DELETE_CATEGORY,
                CREATE_DOCUMENT, READ_DOCUMENT, UPDATE_DOCUMENT, DELETE_DOCUMENT,
                CREATE_EMPLOYEE, READ_EMPLOYEE, UPDATE_EMPLOYEE, DELETE_EMPLOYEE,
                CREATE_ORDER, READ_ORDER, UPDATE_ORDER, DELETE_ORDER,
                CREATE_REPORT, READ_REPORT, UPDATE_REPORT, DELETE_REPORT,
                CREATE_USER, READ_USER, UPDATE_USER, DELETE_USER));
        KNOWN_CODES = Collections.unmodifiableSet(s);
    }
}
