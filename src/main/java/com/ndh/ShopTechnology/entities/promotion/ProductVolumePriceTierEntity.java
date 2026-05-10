package com.ndh.ShopTechnology.entities.promotion;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Mix-and-match / giá theo số lượng: khi tổng số lượng sản phẩm trên đơn (gộp mọi dòng) đạt {@code minQuantity}
 * thì áp dụng {@code unitPrice} cho toàn bộ số lượng đó.
 *
 * <p>Ví dụ: minQuantity=1 unitPrice=100000; minQuantity=2 unitPrice=75000 → mua 2 cái = 150000.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "product_volume_price_tier",
        uniqueConstraints = @UniqueConstraint(name = "uk_volume_tier_product_min_q",
                columnNames = { "product_id", "min_quantity" }))
public class ProductVolumePriceTierEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    /** Từ số lượng này trở lên (theo tổng SL sản phẩm trên đơn) áp dụng bậc giá này. Tối thiểu 1. */
    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity;

    /** Đơn giá (VND) cho mỗi đơn vị khi đạt bậc. */
    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;
}
