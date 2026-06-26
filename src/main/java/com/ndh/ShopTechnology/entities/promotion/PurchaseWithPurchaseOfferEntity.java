package com.ndh.ShopTechnology.entities.promotion;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "purchase_with_purchase_offer",
        uniqueConstraints = @UniqueConstraint(name = "uk_pwp_companion_variant",
                columnNames = "companion_variant_id"))
public class PurchaseWithPurchaseOfferEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anchor_product_id", nullable = false)
    private ProductEntity anchorProduct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anchor_variant_id", nullable = false)
    private ProductVariantEntity anchorVariant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "companion_product_id", nullable = false)
    private ProductEntity companionProduct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "companion_variant_id", nullable = false)
    private ProductVariantEntity companionVariant;

    @Column(name = "promo_unit_price", nullable = false)
    private Double promoUnitPrice;

    @Column(name = "min_anchor_quantity", nullable = false)
    @Builder.Default
    private Integer minAnchorQuantity = 1;

    @Column(name = "companion_promo_units_per_anchor", nullable = false)
    @Builder.Default
    private Integer companionPromoUnitsPerAnchor = 1;

    @Column(name = "max_companion_promo_units")
    private Integer maxCompanionPromoUnits;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /** Thời điểm bắt đầu áp dụng ưu đãi mua kèm. null = áp dụng ngay (không giới hạn đầu). */
    @Column(name = "start_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startAt;

    /** Thời điểm kết thúc. null = không giới hạn cuối. */
    @Column(name = "end_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endAt;
}
