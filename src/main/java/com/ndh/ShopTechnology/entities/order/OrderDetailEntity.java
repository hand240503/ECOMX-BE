package com.ndh.ShopTechnology.entities.order;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "OrderDetail")
@Table(name = "order_detail")
public class OrderDetailEntity extends BaseEntity {

    public static final String COL_DESCRIPTION      = "description";
    public static final String COL_QUANTITY         = "quantity";
    public static final String COL_TOTAL_PRICE      = "total_price";
    public static final String COL_UNIT_PRICE         = "unit_price";
    public static final String COL_ORDER_ID         = "order_id";
    public static final String COL_PRODUCT_ID           = "product_id";
    public static final String COL_PRODUCT_VARIANT_ID   = "product_variant_id";
    public static final String COL_PRICING_PROGRAMS_JSON = "pricing_programs_json";

    @Column(name = COL_DESCRIPTION, nullable = true)
    private String description;

    @Column(name = COL_QUANTITY, nullable = true)
    private Integer quantity;

    @Column(name = COL_UNIT_PRICE, nullable = false)
    private Double unitPrice;

    @Column(name = COL_TOTAL_PRICE, nullable = true)
    private String totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_ORDER_ID, nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_PRODUCT_ID, nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_PRODUCT_VARIANT_ID, nullable = true)
    private ProductVariantEntity productVariant;

    @Column(name = COL_PRICING_PROGRAMS_JSON, columnDefinition = "TEXT")
    private String pricingProgramsJson;

}
