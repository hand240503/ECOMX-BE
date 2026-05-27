package com.ndh.ShopTechnology.entities.product;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "product_price_change")
public class ProductPriceChangeEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariantEntity productVariant;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "base_price", nullable = false)
    private Double basePrice;

    @Column(name = "sale_price")
    private Double salePrice;

    @Column(name = "start_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startAt;

    @Column(name = "end_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endAt;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "quantity_limit")
    private Integer quantityLimit;

    @Column(name = "sold_quantity", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    @Builder.Default
    private Integer soldQuantity = 0;

    @Column(name = "max_per_customer")
    private Integer maxPerCustomer;

    @Column(name = "required_payment_method_code", length = 64)
    private String requiredPaymentMethodCode;
}
