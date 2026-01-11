package com.ndh.ShopTechnology.entities.order;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "warehouse")
public class WarehouseEntity extends BaseEntity {

    public static final String COL_QUANTITY         = "quantity";
    public static final String COL_SUM_BEGIN        = "sum_begin";
    public static final String COL_SUM_END          = "sum_end";
    public static final String COL_ORDER_DETAIL_ID  = "order_detail_id";

    @Column(name = COL_QUANTITY, nullable = true)
    private Integer quantity;

    @Column(name = COL_SUM_BEGIN, nullable = true)
    private Integer sumBegin;

    @Column(name = COL_SUM_END, nullable = true)
    private Integer sumEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_ORDER_DETAIL_ID, nullable = false)
    private OrderDetailEntity orderDetail;

}