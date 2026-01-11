package com.ndh.ShopTechnology.entities.product;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "unit")
public class UnitEntity extends BaseEntity {

    public static final String COL_NAME_UNIT      = "name_unit";
    public static final String COL_RATIO          = "ratio";
    public static final String COL_STATUS         = "status";

    @Column(name = COL_NAME_UNIT, nullable = true)
    private String nameUnit;

    @Column(name = COL_RATIO, nullable = true)
    private Integer ratio;

    @Column(name = COL_STATUS, nullable = true)
    private Integer status;
}
