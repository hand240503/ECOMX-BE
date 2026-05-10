package com.ndh.ShopTechnology.constants;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mô tả chức năng của từng permission code (tiếng Việt).
 *
 * <p>Dùng làm nguồn cho cả tài liệu ({@code docs/Role.md}) và API
 * {@code GET /api/v1/admin/permissions/catalog} (mỗi entry trả về kèm trường {@code description}).
 *
 * <p>Lưu ý:
 * <ul>
 *   <li>Mã 3 chữ số (101..104, 110..112) là quyền hệ thống — bao trùm mọi module.</li>
 *   <li>Mã 6 chữ số (MMMAAA) là quyền theo từng module + action cụ thể.</li>
 * </ul>
 */
public final class PermissionDescriptions {

    private PermissionDescriptions() {
    }

    private static final Map<Integer, String> DESCRIPTIONS;

    static {
        Map<Integer, String> m = new LinkedHashMap<>();

        // ----- System-wide -----
        m.put(PermissionCode.CREATE_ALL,        "Tạo dữ liệu trên toàn bộ module (Product, Category, Document, Employee, Order, Report, ...)");
        m.put(PermissionCode.READ_ALL,          "Đọc dữ liệu trên toàn bộ module");
        m.put(PermissionCode.UPDATE_ALL,        "Cập nhật dữ liệu trên toàn bộ module");
        m.put(PermissionCode.DELETE_ALL,        "Xoá dữ liệu trên toàn bộ module");
        m.put(PermissionCode.LOCK_USER,         "Khoá / mở khoá tài khoản user");
        m.put(PermissionCode.MANAGE_ROLE,       "Quản lý role: tạo / sửa / xoá role và permission mặc định của role");
        m.put(PermissionCode.GRANT_PERMISSION,  "Cấp / thu hồi quyền cho user khác (không vượt quá quyền của bản thân)");

        // ----- Product (100xxx) -----
        m.put(PermissionCode.CREATE_PRODUCT,    "Tạo sản phẩm mới");
        m.put(PermissionCode.READ_PRODUCT,      "Xem danh sách / chi tiết sản phẩm (catalog)");
        m.put(PermissionCode.UPDATE_PRODUCT,    "Cập nhật thông tin sản phẩm");
        m.put(PermissionCode.DELETE_PRODUCT,    "Xoá sản phẩm");

        // ----- Price (150xxx) -----
        m.put(PermissionCode.CREATE_PRICE,      "Thêm giá cho sản phẩm: giá catalog, price change, mix-and-match (volume tier), purchase-with-purchase");
        m.put(PermissionCode.READ_PRICE,        "Xem cấu hình giá sản phẩm: giá catalog, price change, volume tier, PwP");
        m.put(PermissionCode.UPDATE_PRICE,      "Cập nhật cấu hình giá sản phẩm: chỉnh giá catalog, price change, volume tier, PwP");
        m.put(PermissionCode.DELETE_PRICE,      "Xoá cấu hình giá sản phẩm: gỡ giá catalog, price change, volume tier, PwP");

        // ----- Unit (160xxx) -----
        m.put(PermissionCode.CREATE_UNIT,       "Tạo đơn vị tính (cái, thùng, kg, …)");
        m.put(PermissionCode.READ_UNIT,         "Xem danh mục đơn vị tính");
        m.put(PermissionCode.UPDATE_UNIT,       "Cập nhật đơn vị tính");
        m.put(PermissionCode.DELETE_UNIT,       "Xoá đơn vị tính (chặn nếu đang gắn giá sản phẩm)");

        // ----- Brand (170xxx) -----
        m.put(PermissionCode.CREATE_BRAND,      "Tạo thương hiệu / hãng sản phẩm");
        m.put(PermissionCode.READ_BRAND,       "Xem danh mục hãng sản phẩm");
        m.put(PermissionCode.UPDATE_BRAND,     "Cập nhật hãng sản phẩm");
        m.put(PermissionCode.DELETE_BRAND,     "Xoá hãng (chặn nếu còn sản phẩm gắn hãng này)");

        // ----- Category (200xxx) -----
        m.put(PermissionCode.CREATE_CATEGORY,   "Tạo danh mục mới");
        m.put(PermissionCode.READ_CATEGORY,     "Xem danh mục");
        m.put(PermissionCode.UPDATE_CATEGORY,   "Cập nhật danh mục");
        m.put(PermissionCode.DELETE_CATEGORY,   "Xoá danh mục");

        // ----- Document (300xxx) -----
        m.put(PermissionCode.CREATE_DOCUMENT,   "Upload tài liệu / hình ảnh");
        m.put(PermissionCode.READ_DOCUMENT,     "Xem / tải tài liệu");
        m.put(PermissionCode.UPDATE_DOCUMENT,   "Cập nhật tài liệu");
        m.put(PermissionCode.DELETE_DOCUMENT,   "Xoá tài liệu");

        // ----- Employee (400xxx) -----
        m.put(PermissionCode.CREATE_EMPLOYEE,   "Tạo nhân viên mới (user không phải Customer)");
        m.put(PermissionCode.READ_EMPLOYEE,     "Xem danh sách nhân viên");
        m.put(PermissionCode.UPDATE_EMPLOYEE,   "Cập nhật thông tin nhân viên");
        m.put(PermissionCode.DELETE_EMPLOYEE,   "Xoá nhân viên");

        // ----- Order (500xxx) -----
        m.put(PermissionCode.CREATE_ORDER,      "Tạo đơn hàng");
        m.put(PermissionCode.READ_ORDER,        "Xem danh sách / chi tiết đơn hàng");
        m.put(PermissionCode.UPDATE_ORDER,      "Cập nhật trạng thái đơn hàng (xác nhận, giao, hoàn, ...)");
        m.put(PermissionCode.DELETE_ORDER,      "Xoá / huỷ đơn hàng (admin)");

        // ----- Report (600xxx) -----
        m.put(PermissionCode.CREATE_REPORT,     "Tạo báo cáo công việc");
        m.put(PermissionCode.READ_REPORT,       "Xem báo cáo");
        m.put(PermissionCode.UPDATE_REPORT,     "Cập nhật chi tiết báo cáo");
        m.put(PermissionCode.DELETE_REPORT,     "Xoá báo cáo");

        // ----- User (700xxx) -----
        m.put(PermissionCode.CREATE_USER,       "Tạo user (admin)");
        m.put(PermissionCode.READ_USER,         "Xem thông tin user");
        m.put(PermissionCode.UPDATE_USER,       "Cập nhật thông tin user");
        m.put(PermissionCode.DELETE_USER,       "Xoá user");

        DESCRIPTIONS = Collections.unmodifiableMap(m);
    }

    /**
     * Mô tả của 1 code. Trả về fallback "Permission #<code>" nếu không có khai báo (vd code do bạn tự thêm trong DB).
     */
    public static String describe(Integer code) {
        if (code == null) return "";
        String d = DESCRIPTIONS.get(code);
        return d != null ? d : "Permission #" + code;
    }

    /** Bản đồ đầy đủ (read-only). */
    public static Map<Integer, String> asMap() {
        return DESCRIPTIONS;
    }
}
