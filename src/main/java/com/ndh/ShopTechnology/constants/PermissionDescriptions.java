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
        m.put(PermissionCode.CREATE_ALL,        "Tạo dữ liệu trên toàn bộ phân hệ (kể cả nhóm catalogue sản phẩm và quản lý user đã gộp trong bảng phân quyền)");
        m.put(PermissionCode.READ_ALL,          "Đọc dữ liệu trên toàn bộ module");
        m.put(PermissionCode.UPDATE_ALL,        "Cập nhật dữ liệu trên toàn bộ module");
        m.put(PermissionCode.DELETE_ALL,        "Xoá dữ liệu trên toàn bộ module");

        // ----- Product (100xxx): catalogue gộp sản phẩm + giá + đơn vị + hãng + danh mục -----
        m.put(PermissionCode.CREATE_PRODUCT, "Tạo dữ liệu nhóm catalogue: sản phẩm, ảnh/media gắn sản phẩm·variant·danh mục·hãng, giá catalog / price change / volume tier / PwP, đơn vị tính, thương hiệu, danh mục");
        m.put(PermissionCode.READ_PRODUCT,     "Xem toàn nhóm catalogue: sản phẩm, ảnh đính kèm, cấu hình giá, đơn vị tính, hãng, danh mục");
        m.put(PermissionCode.UPDATE_PRODUCT,   "Cập nhật dữ liệu nhóm catalogue: sản phẩm, ảnh/media gắn catalogue (thay file, đặt main, meta), giá và chương trình khuyến mãi liên quan giá, đơn vị, hãng, danh mục");
        m.put(PermissionCode.DELETE_PRODUCT,  "Xoá dữ liệu trong nhóm catalogue (sản phẩm, bản media gắn catalogue trong kho lưu tài liệu, giá và cấu hình liên quan, đơn vị — khi không còn ràng buộc, hãng, danh mục)");

        // ----- Legacy branch codes (still valid in grants / DB until migrated); mô tả trùng nhóm -----
        m.put(PermissionCode.CREATE_PRICE,    m.get(PermissionCode.CREATE_PRODUCT));
        m.put(PermissionCode.READ_PRICE,       m.get(PermissionCode.READ_PRODUCT));
        m.put(PermissionCode.UPDATE_PRICE,     m.get(PermissionCode.UPDATE_PRODUCT));
        m.put(PermissionCode.DELETE_PRICE,     m.get(PermissionCode.DELETE_PRODUCT));
        m.put(PermissionCode.CREATE_UNIT,     m.get(PermissionCode.CREATE_PRODUCT));
        m.put(PermissionCode.READ_UNIT,        m.get(PermissionCode.READ_PRODUCT));
        m.put(PermissionCode.UPDATE_UNIT,      m.get(PermissionCode.UPDATE_PRODUCT));
        m.put(PermissionCode.DELETE_UNIT,      m.get(PermissionCode.DELETE_PRODUCT));
        m.put(PermissionCode.CREATE_BRAND,     m.get(PermissionCode.CREATE_PRODUCT));
        m.put(PermissionCode.READ_BRAND,      m.get(PermissionCode.READ_PRODUCT));
        m.put(PermissionCode.UPDATE_BRAND,     m.get(PermissionCode.UPDATE_PRODUCT));
        m.put(PermissionCode.DELETE_BRAND,     m.get(PermissionCode.DELETE_PRODUCT));
        m.put(PermissionCode.CREATE_CATEGORY,  m.get(PermissionCode.CREATE_PRODUCT));
        m.put(PermissionCode.READ_CATEGORY,    m.get(PermissionCode.READ_PRODUCT));
        m.put(PermissionCode.UPDATE_CATEGORY,  m.get(PermissionCode.UPDATE_PRODUCT));
        m.put(PermissionCode.DELETE_CATEGORY,  m.get(PermissionCode.DELETE_PRODUCT));

        // ----- Document (300xxx) -----
        m.put(PermissionCode.CREATE_DOCUMENT,   "Upload tài liệu / ảnh khi không dùng quyền nhóm catalogue; hoặc media gắn đơn hàng / hồ sơ user riêng (ảnh SKU·danh mục·hãng có thể chỉ cần quyền sản phẩm)");
        m.put(PermissionCode.READ_DOCUMENT,     "Xem / tải tài liệu");
        m.put(PermissionCode.UPDATE_DOCUMENT,   "Cập nhật tài liệu");
        m.put(PermissionCode.DELETE_DOCUMENT,   "Xoá tài liệu");

        // ----- User admin (700xxx): mọi tài khoản trong hệ thống -----
        m.put(PermissionCode.CREATE_USER, "Tạo tài khoản qua API quản trị nơi có hỗ trợ (staff/employee/v.v.); quyền này không thay thế luồng đăng ký khách trên storefront.");
        m.put(PermissionCode.READ_USER,   "Xem và liệt kê toàn bộ user trong hệ thống (khách và nội bộ)");
        m.put(PermissionCode.UPDATE_USER, "Cập nhật thông tin user bất kỳ (profile, khóa, role trong API admin, …)");
        m.put(PermissionCode.DELETE_USER,  "Xoá / vô hiệu hoá user bất kỳ trong phạm vi API admin được phép");

        // ----- Employee (400xxx): legacy branch — đồng nghĩa catalogue nhóm USER (700xxx), phạm vi mọi user -----
        m.put(PermissionCode.CREATE_EMPLOYEE, m.get(PermissionCode.CREATE_USER));
        m.put(PermissionCode.READ_EMPLOYEE,     m.get(PermissionCode.READ_USER));
        m.put(PermissionCode.UPDATE_EMPLOYEE,   m.get(PermissionCode.UPDATE_USER));
        m.put(PermissionCode.DELETE_EMPLOYEE,   m.get(PermissionCode.DELETE_USER));

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

        DESCRIPTIONS = Collections.unmodifiableMap(m);
    }

    /**
     * Mô tả của 1 code. Trả về fallback "Permission #<code>" nếu không có khai báo (vd code do bạn tự thêm trong DB).
     */
    public static String describe(Integer code) {
        if (code == null) return "";
        String d = DESCRIPTIONS.get(code);
        if (d != null) return d;
        int canon = PermissionCode.normalizeGrantPermissionCode(code);
        if (canon != code) {
            d = DESCRIPTIONS.get(canon);
            if (d != null) return d;
        }
        return "Permission #" + code;
    }

    /** Bản đồ đầy đủ (read-only). */
    public static Map<Integer, String> asMap() {
        return DESCRIPTIONS;
    }
}
