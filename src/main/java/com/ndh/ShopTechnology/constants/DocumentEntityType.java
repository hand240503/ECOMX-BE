package com.ndh.ShopTechnology.constants;

/**
 * <p><b>Đăng ký quy chuẩn {@code document.entity_type}</b> — số nguyên trong DB; {@code document.entity_id} là
 * khóa chính của bảng tương ứng (không FK trong schema nhưng ý nghĩa tham chiếu rõ ràng).
 *
 * <p>Tên hằng dùng tiền tố {@code ID_DOCUMENT_ENTITY_*} (tương đương bảng “định nghĩa” loại thực thể).
 *
 * <p><b>Bảng đăng ký (đồng bộ FE – BE – migration SQL):</b>
 * <table border="1" summary="entity_type registry">
 *   <tr><th>{@code entity_type}</th><th>Hằng Java</th><th>Bảng</th><th>{@code entity_id}</th></tr>
 *   <tr><td>{@code -1}</td><td>{@link #ID_DOCUMENT_ENTITY_UNASSIGNED}</td><td>—</td><td>không FK</td></tr>
 *   <tr><td>{@code 100000}</td><td>{@link #ID_DOCUMENT_ENTITY_PRODUCT}</td><td>{@code products}</td><td>{@code products.id}</td></tr>
 *   <tr><td>{@code 105000}</td><td>{@link #ID_DOCUMENT_ENTITY_PRODUCT_VARIANT}</td><td>{@code product_variant}</td><td>{@code product_variant.id}</td></tr>
 *   <tr><td>{@code 200000}</td><td>{@link #ID_DOCUMENT_ENTITY_USER}</td><td>{@code users}</td><td>{@code users.id}</td></tr>
 *   <tr><td>{@code 300000}</td><td>{@link #ID_DOCUMENT_ENTITY_CATEGORY}</td><td>{@code category}</td><td>{@code category.id}</td></tr>
 *   <tr><td>{@code 400000}</td><td>{@link #ID_DOCUMENT_ENTITY_BRAND}</td><td>{@code brands}</td><td>{@code brands.id}</td></tr>
 *   <tr><td>{@code 500000}</td><td>{@link #ID_DOCUMENT_ENTITY_ORDER}</td><td>{@code orders}</td><td>{@code orders.id}</td></tr>
 * </table>
 *
 * <p>Mở rộng: thêm hằng {@code 600000}, {@code 700000}, … theo nhóm module; cập nhật {@link #isRegistered(int)}.
 */
public final class DocumentEntityType {

    private DocumentEntityType() {
    }

    /** Chưa gắn đối tượng / legacy; {@code entity_id} không mang ý nghĩa khóa ngoại. Tương đương ý nghĩa cũ {@code UNASSIGNED}. */
    public static final int ID_DOCUMENT_ENTITY_UNASSIGNED = -1;

    /** Ảnh, video, tài liệu sản phẩm (SPU). */
    public static final int ID_DOCUMENT_ENTITY_PRODUCT = 100_000;

    /** Ảnh/media riêng từng biến thể (SKU); {@code entity_id} = {@code product_variant.id}. */
    public static final int ID_DOCUMENT_ENTITY_PRODUCT_VARIANT = 105_000;

    /** Tài liệu / ảnh gắn người dùng. */
    public static final int ID_DOCUMENT_ENTITY_USER = 200_000;

    /** Ảnh minh họa danh mục. */
    public static final int ID_DOCUMENT_ENTITY_CATEGORY = 300_000;

    /** Logo / tài liệu thương hiệu. */
    public static final int ID_DOCUMENT_ENTITY_BRAND = 400_000;

    /** Đính kèm đơn hàng. */
    public static final int ID_DOCUMENT_ENTITY_ORDER = 500_000;

    /**
     * @deprecated Dùng {@link #ID_DOCUMENT_ENTITY_UNASSIGNED}.
     */
    @Deprecated
    public static final int UNASSIGNED = ID_DOCUMENT_ENTITY_UNASSIGNED;

    /**
     * @deprecated Dùng {@link #ID_DOCUMENT_ENTITY_PRODUCT}. Giá trị cũ {@code 1} không còn dùng.
     */
    @Deprecated
    public static final int PRODUCT = ID_DOCUMENT_ENTITY_PRODUCT;

    /**
     * @deprecated Dùng {@link #ID_DOCUMENT_ENTITY_USER}.
     */
    @Deprecated
    public static final int USER = ID_DOCUMENT_ENTITY_USER;

    /**
     * @deprecated Dùng {@link #ID_DOCUMENT_ENTITY_CATEGORY}.
     */
    @Deprecated
    public static final int CATEGORY = ID_DOCUMENT_ENTITY_CATEGORY;

    /**
     * @deprecated Dùng {@link #ID_DOCUMENT_ENTITY_BRAND}.
     */
    @Deprecated
    public static final int BRAND = ID_DOCUMENT_ENTITY_BRAND;

    /**
     * @deprecated Dùng {@link #ID_DOCUMENT_ENTITY_ORDER}.
     */
    @Deprecated
    public static final int ORDER = ID_DOCUMENT_ENTITY_ORDER;

    /** Mã dương nhỏ nhất trong đăng ký hiện tại. */
    public static final int MIN_REGISTERED_POSITIVE_TYPE = ID_DOCUMENT_ENTITY_PRODUCT;

    /** Mã dương lớn nhất trong đăng ký hiện tại. */
    public static final int MAX_REGISTERED_POSITIVE_TYPE = ID_DOCUMENT_ENTITY_ORDER;

    /**
     * {@code true} nếu {@code type} là một trong các mã đã định nghĩa (kể cả {@link #ID_DOCUMENT_ENTITY_UNASSIGNED}).
     */
    public static boolean isRegistered(int type) {
        return switch (type) {
            case ID_DOCUMENT_ENTITY_UNASSIGNED,
                    ID_DOCUMENT_ENTITY_PRODUCT,
                    ID_DOCUMENT_ENTITY_PRODUCT_VARIANT,
                    ID_DOCUMENT_ENTITY_USER,
                    ID_DOCUMENT_ENTITY_CATEGORY,
                    ID_DOCUMENT_ENTITY_BRAND,
                    ID_DOCUMENT_ENTITY_ORDER -> true;
            default -> false;
        };
    }
}
