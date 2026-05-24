package com.ndh.ShopTechnology.entities.promotion;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Mix-and-match / giá theo bậc số lượng theo **phân loại (SKU)**: khi tổng số lượng **đúng variant đó** trên đơn
 * đạt {@code minQuantity} thì áp {@code unitPrice} cho các đơn vị của variant đó (không gộp chéo SKU khác).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "product_volume_price_tier",
        uniqueConstraints = @UniqueConstraint(name = "uk_volume_tier_variant_min_q",
                columnNames = { "product_variant_id", "min_quantity" }))
public class ProductVolumePriceTierEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariantEntity productVariant;

    /** Từ số lượng này trở lên (theo tổng SL **variant** trên đơn) áp dụng bậc giá này. */
    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;
}
