package com.ndh.ShopTechnology.entities.promotion;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Purchase-with-purchase: có đủ sản phẩm neo (anchor) trên đơn thì một phần số lượng sản phẩm đi kèm (companion)
 * được tính theo {@code promoUnitPrice}.
 *
 * <p>Mặc định tỉ lệ 1:1 — mỗi đơn vị anchor “mở” một đơn vị companion giá khuyến mãi (trong giới hạn tổng SL companion).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "purchase_with_purchase_offer",
        uniqueConstraints = @UniqueConstraint(name = "uk_pwp_companion",
                columnNames = "companion_product_id"))
public class PurchaseWithPurchaseOfferEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anchor_product_id", nullable = false)
    private ProductEntity anchorProduct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "companion_product_id", nullable = false)
    private ProductEntity companionProduct;

    /** Đơn giá khuyến mãi cho companion (VND). */
    @Column(name = "promo_unit_price", nullable = false)
    private Double promoUnitPrice;

    /**
     * Tổng số lượng anchor trên đơn tối thiểu để kích hoạt (ví dụ 1).
     */
    @Column(name = "min_anchor_quantity", nullable = false)
    @Builder.Default
    private Integer minAnchorQuantity = 1;

    /**
     * Số đơn vị companion được giá promo cho mỗi 1 đơn vị anchor (mặc định 1).
     */
    @Column(name = "companion_promo_units_per_anchor", nullable = false)
    @Builder.Default
    private Integer companionPromoUnitsPerAnchor = 1;

    /** Trần tổng số đơn vị companion được giá promo trên một đơn; null = không giới hạn. */
    @Column(name = "max_companion_promo_units")
    private Integer maxCompanionPromoUnits;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;
}
