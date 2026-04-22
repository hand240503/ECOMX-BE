package com.ndh.ShopTechnology.entities.product;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "products")
public class ProductEntity extends BaseEntity {

    public static final String COL_PRODUCT_NAME = "product_name";
    public static final String COL_STATUS = "status";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_CATEGORY_ID = "category_id";
    public static final String COL_PRODUCT_ID = "product_id";
    public static final String COL_BRAND_ID = "brand_id";
    public static final String COL_IS_FEATURED = "is_featured";
    public static final String COL_SOLD_COUNT = "sold_count";
    public static final String COL_TAG = "tag";

    @Column(name = COL_PRODUCT_NAME, nullable = true)
    private String productName;

    @Column(name = COL_STATUS, nullable = true)
    private Integer status;

    @Column(name = COL_DESCRIPTION, nullable = true)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_CATEGORY_ID, nullable = false)
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_BRAND_ID)
    private BrandEntity brand;

    @Column(name = COL_IS_FEATURED)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = COL_SOLD_COUNT)
    @Builder.Default
    private Long soldCount = 0L;

    @Column(name = COL_TAG)
    private String tag;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 32)
    @Builder.Default
    private java.util.List<PriceEntity> prices = new java.util.ArrayList<>();

    /**
     * Chính sách áp dụng cho sản phẩm (cùng một bản ghi {@link PolicyEntity} có thể dùng cho nhiều SP).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_policies",
        joinColumns = @JoinColumn(name = "product_id", referencedColumnName = "id"),
        inverseJoinColumns = @JoinColumn(name = "policy_id", referencedColumnName = "id"))
    @BatchSize(size = 32)
    @Builder.Default
    private Set<PolicyEntity> policies = new HashSet<>();
}
