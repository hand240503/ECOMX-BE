package com.ndh.ShopTechnology.entities.store;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Tồn kho theo từng kho (store) cho mỗi biến thể (SKU).
 *
 * <p>Mỗi dòng là tổ hợp duy nhất (store, variant) với {@code onHand} (tồn thực)
 * và {@code reserved} (đang giữ cho đơn). Số bán được = onHand - reserved.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "store_stock")
@Table(
        name = "store_stock",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_store_stock_store_variant",
                columnNames = {"store_id", "variant_id"}),
        indexes = {
                @Index(name = "idx_store_stock_store", columnList = "store_id"),
                @Index(name = "idx_store_stock_variant", columnList = "variant_id")
        })
public class StoreStockEntity extends BaseEntity {

    public static final String COL_STORE_ID   = "store_id";
    public static final String COL_VARIANT_ID = "variant_id";
    public static final String COL_ON_HAND    = "on_hand";
    public static final String COL_RESERVED   = "reserved";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COL_STORE_ID, nullable = false)
    private StoreEntity store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COL_VARIANT_ID, nullable = false)
    private ProductVariantEntity variant;

    @Column(name = COL_ON_HAND, nullable = false)
    @Builder.Default
    private Integer onHand = 0;

    @Column(name = COL_RESERVED, nullable = false)
    @Builder.Default
    private Integer reserved = 0;

    @Transient
    public int getAvailable() {
        int oh = onHand != null ? onHand : 0;
        int rs = reserved != null ? reserved : 0;
        return oh - rs;
    }
}
