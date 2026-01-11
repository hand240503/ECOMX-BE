package com.ndh.ShopTechnology.entities.product;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.order.OrderEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "products")
public class ProductEntity extends BaseEntity {

    public static final String COL_PRODUCT_NAME     = "product_name";
    public static final String COL_STATUS           = "status";
    public static final String COL_DESCRIPTION      = "description";
    public static final String COL_CATEGORY_ID      = "category_id";
    public static final String COL_PRODUCT_ID       = "product_id";

    @Column(name = COL_PRODUCT_NAME, nullable = true)
    private String productName;

    @Column(name = COL_STATUS, nullable = true)
    private String status;

    @Column(name = COL_DESCRIPTION, nullable = true)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_CATEGORY_ID, nullable = false)
    private CategoryEntity category;

}
