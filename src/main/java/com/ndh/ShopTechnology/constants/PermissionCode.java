package com.ndh.ShopTechnology.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Định nghĩa toàn bộ mã quyền (permission code) của hệ thống.
 *
 * <p>Quy tắc mã quyền:
 * <ul>
 *   <li>Mã 3 chữ số bắt đầu bằng <b>1</b> (101..104, 110..112): áp dụng cho TOÀN BỘ module ("system-wide").</li>
 *   <li>Mã 6 chữ số: áp dụng cho từng module — cấu trúc <code>MMMAAA</code> với
 *       <ul>
 *         <li><b>MMM</b> = mã module</li>
 *         <li><b>AAA</b> = mã action (001 Create, 002 Read, 003 Update, 004 Delete)</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p>Ví dụ: <code>100002</code> = Read Product. <code>200003</code> = Update Category.
 *
 * <p>Lưu ý: User có quyền hệ thống <code>102</code> (Read all) sẽ tự động đọc được mọi module
 * (xem {@code PermissionEvaluator#hasPermission}).
 */
public final class PermissionCode {

    private PermissionCode() {
    }

    // =====================================================================
    // SYSTEM-WIDE (3 chữ số, prefix 1)
    // =====================================================================
    public static final int CREATE_ALL = 101;
    public static final int READ_ALL   = 102;
    public static final int UPDATE_ALL = 103;
    public static final int DELETE_ALL = 104;

    /** Khóa / mở khóa tài khoản user (dành cho Super Admin / Admin). */
    public static final int LOCK_USER          = 110;
    /** Quản lý role (tạo / sửa / xóa role + permission mặc định của role). */
    public static final int MANAGE_ROLE        = 111;
    /** Cấp / thu hồi quyền cho user khác. */
    public static final int GRANT_PERMISSION   = 112;

    // =====================================================================
    // MODULE CODES (MMM)
    // =====================================================================
    public static final int MODULE_PRODUCT    = 100;
    /**
     * Module quản trị giá: bao trùm 4 nghiệp vụ giá của sản phẩm
     * (giá catalog hằng ngày, price change theo thời gian, volume tier mix-and-match,
     * purchase-with-purchase). Tách riêng khỏi MODULE_PRODUCT để có thể giao
     * quyền "đổi giá / chạy khuyến mãi" mà không cần mở quyền sửa toàn bộ sản phẩm.
     */
    public static final int MODULE_PRICE      = 150;
    /** Đơn vị tính sản phẩm (cái, thùng, kg, …) — dùng khi tạo giá catalog. */
    public static final int MODULE_UNIT       = 160;
    /** Thương hiệu / hãng sản phẩm — dùng khi tạo / sửa sản phẩm ({@code brand_id}). */
    public static final int MODULE_BRAND      = 170;
    public static final int MODULE_CATEGORY   = 200;
    public static final int MODULE_DOCUMENT   = 300;
    public static final int MODULE_EMPLOYEE   = 400;
    public static final int MODULE_ORDER      = 500;
    public static final int MODULE_REPORT     = 600;
    public static final int MODULE_USER       = 700;
    public static final int MODULE_ROLE       = 800;
    public static final int MODULE_PERMISSION = 900;

    // =====================================================================
    // ACTION CODES (AAA)
    // =====================================================================
    public static final int ACTION_CREATE = 1;
    public static final int ACTION_READ   = 2;
    public static final int ACTION_UPDATE = 3;
    public static final int ACTION_DELETE = 4;

    // =====================================================================
    // MODULE-SPECIFIC (6 chữ số) — Generated qua moduleAction(...)
    // =====================================================================

    // Product (100xxx)
    public static final int CREATE_PRODUCT = 100001;
    public static final int READ_PRODUCT   = 100002;
    public static final int UPDATE_PRODUCT = 100003;
    public static final int DELETE_PRODUCT = 100004;

    // Price (150xxx) — quản lý mọi loại giá của sản phẩm
    public static final int CREATE_PRICE = 150001;
    public static final int READ_PRICE   = 150002;
    public static final int UPDATE_PRICE = 150003;
    public static final int DELETE_PRICE = 150004;

    // Unit (160xxx)
    public static final int CREATE_UNIT = 160001;
    public static final int READ_UNIT   = 160002;
    public static final int UPDATE_UNIT = 160003;
    public static final int DELETE_UNIT = 160004;

    // Brand (170xxx)
    public static final int CREATE_BRAND = 170001;
    public static final int READ_BRAND   = 170002;
    public static final int UPDATE_BRAND = 170003;
    public static final int DELETE_BRAND = 170004;

    // Category (200xxx)
    public static final int CREATE_CATEGORY = 200001;
    public static final int READ_CATEGORY   = 200002;
    public static final int UPDATE_CATEGORY = 200003;
    public static final int DELETE_CATEGORY = 200004;

    // Document (300xxx)
    public static final int CREATE_DOCUMENT = 300001;
    public static final int READ_DOCUMENT   = 300002;
    public static final int UPDATE_DOCUMENT = 300003;
    public static final int DELETE_DOCUMENT = 300004;

    // Employee (400xxx) — quản lý nhân viên (user không phải Customer)
    public static final int CREATE_EMPLOYEE = 400001;
    public static final int READ_EMPLOYEE   = 400002;
    public static final int UPDATE_EMPLOYEE = 400003;
    public static final int DELETE_EMPLOYEE = 400004;

    // Order (500xxx)
    public static final int CREATE_ORDER = 500001;
    public static final int READ_ORDER   = 500002;
    public static final int UPDATE_ORDER = 500003;
    public static final int DELETE_ORDER = 500004;

    // Report (600xxx) — job report
    public static final int CREATE_REPORT = 600001;
    public static final int READ_REPORT   = 600002;
    public static final int UPDATE_REPORT = 600003;
    public static final int DELETE_REPORT = 600004;

    // User (700xxx) — thông tin người dùng (cho admin)
    public static final int CREATE_USER = 700001;
    public static final int READ_USER   = 700002;
    public static final int UPDATE_USER = 700003;
    public static final int DELETE_USER = 700004;

    // =====================================================================
    // HELPERS
    // =====================================================================

    /**
     * Compose module-action code: <code>MMM * 1000 + AAA</code>.
     * Ví dụ: {@code moduleAction(100, 2)} → 100002 (Read Product).
     */
    public static int moduleAction(int moduleCode, int actionCode) {
        if (moduleCode < 100 || moduleCode > 999) {
            throw new IllegalArgumentException("moduleCode must be 3 digits: " + moduleCode);
        }
        if (actionCode < 1 || actionCode > 999) {
            throw new IllegalArgumentException("actionCode must be 1..999: " + actionCode);
        }
        return moduleCode * 1000 + actionCode;
    }

    /** True nếu code là 3 chữ số (100..999) — system-wide. */
    public static boolean isSystemWide(int code) {
        return code >= 100 && code <= 999;
    }

    /** True nếu code là 6 chữ số (100000..999999) — module-specific. */
    public static boolean isModuleSpecific(int code) {
        return code >= 100_000 && code <= 999_999;
    }

    /** Trích action từ mã 6 chữ số (vd 100002 → 2). Trả -1 nếu không phải mã 6 chữ số. */
    public static int extractAction(int code) {
        if (!isModuleSpecific(code)) return -1;
        return code % 1000;
    }

    /** Trích module từ mã 6 chữ số (vd 100002 → 100). Trả -1 nếu không phải mã 6 chữ số. */
    public static int extractModule(int code) {
        if (!isModuleSpecific(code)) return -1;
        return code / 1000;
    }

    /**
     * Mã system-wide (101..104) tương ứng với action của một mã 6 chữ số. Vd 100003 → 103 (UPDATE_ALL).
     * Trả -1 nếu không có ánh xạ (vd code không phải CRUD chuẩn).
     */
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

    /**
     * Tập hợp tất cả mã quyền hợp lệ (đã được khai báo). Dùng để validate input từ client.
     */
    public static Set<Integer> allKnownCodes() {
        return KNOWN_CODES;
    }

    private static final Set<Integer> KNOWN_CODES;
    static {
        Set<Integer> s = new LinkedHashSet<>(Arrays.asList(
                CREATE_ALL, READ_ALL, UPDATE_ALL, DELETE_ALL,
                LOCK_USER, MANAGE_ROLE, GRANT_PERMISSION,
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
