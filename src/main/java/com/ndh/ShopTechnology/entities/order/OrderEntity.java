package com.ndh.ShopTechnology.entities.order;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "orders")
public class OrderEntity extends BaseEntity {

    public static final String COL_STATUS         = "status";
    public static final String COL_DESCRIPTION     = "description";
    public static final String COL_TOTAL          = "total";
    public static final String COL_TYPE_ORDER     = "type_order";
    public static final String COL_USER_ID        = "user_id";

    @Column(name = COL_STATUS, nullable = true)
    private Integer status;

    @Column(name = COL_DESCRIPTION, nullable = true)
    private String description;

    @Column(name = COL_TOTAL, nullable = true)
    private Double total;

    @Column(name = COL_TYPE_ORDER, nullable = true)
    private Integer typeOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_USER_ID, nullable = false)
    private UserEntity user;
}