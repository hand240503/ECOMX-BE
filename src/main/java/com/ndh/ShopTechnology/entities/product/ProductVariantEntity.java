package com.ndh.ShopTechnology.entities.product;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "ProductVariant")
@Table(name = "product_variant")
public class ProductVariantEntity extends BaseEntity {

    public static final String COL_PRODUCT_ID = "product_id";
    public static final String COL_SKU_CODE = "sku_code";
    public static final String COL_OPTION_VALUES = "option_values";
    public static final String COL_ACTIVE = "active";
    public static final String COL_SORT_ORDER = "sort_order";
    public static final String COL_ON_HAND = "on_hand";
    public static final String COL_RESERVED = "reserved";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COL_PRODUCT_ID, nullable = false)
    private ProductEntity product;

    @Column(name = COL_SKU_CODE, unique = true, length = 128)
    private String skuCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = COL_OPTION_VALUES, columnDefinition = "json")
    @Builder.Default
    private Map<String, String> optionValues = new LinkedHashMap<>();

    @Column(name = COL_ACTIVE, nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = COL_SORT_ORDER, nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** Số lượng tồn kho thực tế (đã nhập, chưa trừ khi bán). */
    @Column(name = COL_ON_HAND, nullable = false)
    @Builder.Default
    private Integer onHand = 0;

    /** Số lượng đang được giữ cho các đơn chưa hoàn thành (chưa xuất kho). */
    @Column(name = COL_RESERVED, nullable = false)
    @Builder.Default
    private Integer reserved = 0;

    /** Số lượng còn bán được = onHand - reserved. */
    @Transient
    public int getAvailable() {
        int oh = onHand != null ? onHand : 0;
        int rs = reserved != null ? reserved : 0;
        return oh - rs;
    }

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 32)
    @Builder.Default
    private java.util.List<PriceEntity> prices = new java.util.ArrayList<>();
}
