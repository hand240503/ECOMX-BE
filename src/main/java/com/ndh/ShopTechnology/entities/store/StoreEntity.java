package com.ndh.ShopTechnology.entities.store;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Kho / cửa hàng (store). Vừa là điểm xuất hàng (origin tính phí ship), vừa là
 * nơi giữ tồn kho theo từng biến thể (xem {@link StoreStockEntity}).
 *
 * <p>Khách hàng chọn một store khi đặt đơn → phí ship tính từ vị trí store này
 * tới địa chỉ giao, và tồn kho được kiểm tra / trừ tại đúng store đó.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "store")
@Table(
        name = "store",
        uniqueConstraints = @UniqueConstraint(name = "uk_store_code", columnNames = "code"),
        indexes = {
                @Index(name = "idx_store_active", columnList = "active"),
                @Index(name = "idx_store_default", columnList = "is_default")
        })
public class StoreEntity extends BaseEntity {

    public static final String COL_CODE         = "code";
    public static final String COL_NAME         = "name";
    public static final String COL_PHONE        = "phone";
    public static final String COL_ADDRESS_LINE = "address_line";
    public static final String COL_CITY         = "city";
    public static final String COL_LATITUDE     = "latitude";
    public static final String COL_LONGITUDE    = "longitude";
    public static final String COL_ACTIVE       = "active";
    public static final String COL_DEFAULT      = "is_default";
    public static final String COL_NOTE         = "note";

    /** Mã kho duy nhất (ví dụ: HN01, DN02). */
    @Column(name = COL_CODE, nullable = false, length = 64)
    private String code;

    /** Tên hiển thị của kho / cửa hàng. */
    @Column(name = COL_NAME, nullable = false, length = 255)
    private String name;

    @Column(name = COL_PHONE, length = 32)
    private String phone;

    /** Địa chỉ chi tiết (để hiển thị). */
    @Column(name = COL_ADDRESS_LINE, columnDefinition = "TEXT")
    private String addressLine;

    @Column(name = COL_CITY, length = 128)
    private String city;

    /** Toạ độ kho — dùng làm điểm xuất phát khi tính khoảng cách / phí ship. */
    @Column(name = COL_LATITUDE)
    private Double latitude;

    @Column(name = COL_LONGITUDE)
    private Double longitude;

    @Column(name = COL_ACTIVE, nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Kho mặc định — dùng khi đơn không chỉ định store (tương thích client cũ). */
    @Column(name = COL_DEFAULT, nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = COL_NOTE, columnDefinition = "TEXT")
    private String note;
}
