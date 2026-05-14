package com.ndh.ShopTechnology.entities.product;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

/**
 * Price change theo thời gian cho một biến thể (SKU).
 *
 * <p>Giá hiệu lực = (salePrice != null ? salePrice : basePrice) trong khoảng
 * [startAt, endAt] (endAt có thể null = không hết hạn).
 */
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

    /** Trùng {@link ProductVariantEntity#getProduct()} — schema DB giữ cột NOT NULL. */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** Giá gốc (VND). */
    @Column(name = "base_price", nullable = false)
    private Double basePrice;

    /** Giá ưu đãi (VND), null = không giảm. */
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
}

