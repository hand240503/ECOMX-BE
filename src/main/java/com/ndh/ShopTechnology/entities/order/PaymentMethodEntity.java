package com.ndh.ShopTechnology.entities.order;

import com.ndh.ShopTechnology.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "payment_methods")
public class PaymentMethodEntity extends BaseEntity {

    public static final String COL_NAME = "name";
    public static final String COL_CODE = "code";
    public static final String COL_ACTIVE = "active";
    public static final String COL_SORT_ORDER = "sort_order";

    @Column(name = COL_NAME, nullable = false, length = 128)
    private String name;

    /**
     * Mã nội bộ ổn định (VD: COD, BANK_TRANSFER) — dùng khi tích hợp thanh toán sau này.
     */
    @Column(name = COL_CODE, nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = COL_ACTIVE, nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = COL_SORT_ORDER, nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
