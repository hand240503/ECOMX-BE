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
@Entity(name = "brands")
public class BrandEntity extends BaseEntity {

    public static final String COL_CODE   = "code";
    public static final String COL_NAME   = "name";
    public static final String COL_STATUS = "status";

    @Column(name = COL_CODE, nullable = false, unique = true)
    private String code;

    @Column(name = COL_NAME, nullable = false)
    private String name;

    @Column(name = COL_STATUS)
    private Integer status;

    @OneToMany(mappedBy = "brand", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private Set<ProductEntity> products = new HashSet<>();
}
