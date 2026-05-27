package com.ndh.ShopTechnology.entities.promotion;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;
}
