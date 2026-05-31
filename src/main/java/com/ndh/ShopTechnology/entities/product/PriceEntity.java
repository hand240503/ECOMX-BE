package com.ndh.ShopTechnology.entities.product;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "price")
public class PriceEntity extends BaseEntity {

    public static final String COL_CURRENT_VALUE = "current_value";
    public static final String COL_OLD_VALUE = "old_value";
    public static final String COL_PRODUCT_VARIANT_ID = "product_variant_id";
    public static final String COL_UNIT_ID = "unit_id";
    public static final String COL_DISPLAY_NAME = "display_name";

    @Column(name = COL_CURRENT_VALUE, nullable = false)
    private Double currentValue;

    @Column(name = COL_OLD_VALUE, nullable = false)
    private Double oldValue;

    /** Tên hiển thị tuỳ chỉnh cho dòng giá (ví dụ: "Hộp 6 chiếc", "Combo 2+1"). Nullable. */
    @Column(name = COL_DISPLAY_NAME)
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_UNIT_ID, nullable = false)
    private UnitEntity unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_PRODUCT_VARIANT_ID, nullable = false)
    private ProductVariantEntity variant;
}
