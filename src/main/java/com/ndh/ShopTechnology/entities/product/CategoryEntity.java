package com.ndh.ShopTechnology.entities.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "category")
public class CategoryEntity extends BaseEntity {

    public static final String COL_CODE            = "code";
    public static final String COL_NAME            = "name";
    public static final String COL_STATUS          = "status";
    public static final String COL_PARENT_ID       = "parent_id";

    @Column(name = COL_CODE, nullable = true, unique = true)
    private String code;

    @Column(name = COL_NAME, nullable = true)
    private String name;

    @Column(name = COL_STATUS, nullable = true)
    private Integer status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_PARENT_ID, nullable = true)
    @JsonIgnore
    private CategoryEntity parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private Set<CategoryEntity> children = new HashSet<>();

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private Set<ProductEntity> products = new HashSet<>();
}
