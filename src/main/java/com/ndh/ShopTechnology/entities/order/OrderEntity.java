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
@Entity(name = "Order")
@Table(name = "orders")
public class OrderEntity extends BaseEntity {

    public static final String COL_STATUS         = "status";
    public static final String COL_DESCRIPTION     = "description";
    public static final String COL_TOTAL          = "total";
    public static final String COL_TYPE_ORDER     = "type_order";
    public static final String COL_USER_ID        = "user_id";
    public static final String COL_ORDER_CODE     = "order_code";
    public static final String COL_DELIVERY_ADDRESS = "delivery_address";
    public static final String COL_DELIVERY_DISTANCE_METERS = "delivery_distance_meters";
    public static final String COL_SHIPPING_FEE_VND = "shipping_fee_vnd";
    public static final String COL_PAYMENT_METHOD_ID = "payment_method_id";
    public static final String COL_RETURN_REFUND_STATUS = "return_refund_status";
    public static final String COL_RETURN_REFUND_NOTE  = "return_refund_note";
    public static final String COL_PAID = "is_paid";
    public static final String COL_PAID_AT = "paid_at";
    public static final String COL_CHECKOUT_SESSION_PUBLIC_ID = "checkout_session_public_id";
    public static final String COL_VNPAY_CHECKOUT_TXN_REF = "vnpay_checkout_txn_ref";

    @Column(name = COL_STATUS, nullable = true)
    private Integer status;

    @Column(name = COL_RETURN_REFUND_STATUS, nullable = true)
    private Integer returnRefundStatus;

    @Column(name = COL_RETURN_REFUND_NOTE, columnDefinition = "TEXT")
    private String returnRefundNote;

    @Column(name = COL_PAID, nullable = false)
    @Builder.Default
    private Boolean paid = false;

    @Column(name = COL_PAID_AT, nullable = true)
    private java.util.Date paidAt;

    @Column(name = COL_DESCRIPTION, nullable = true)
    private String description;

    @Column(name = COL_TOTAL, nullable = true)
    private Double total;

    @Column(name = COL_TYPE_ORDER, nullable = true)
    private Integer typeOrder;

    @Column(name = COL_ORDER_CODE, nullable = true, unique = true, length = 32)
    private String orderCode;

    @Column(name = COL_DELIVERY_ADDRESS, columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = COL_DELIVERY_DISTANCE_METERS)
    private Double deliveryDistanceMeters;

    @Column(name = COL_SHIPPING_FEE_VND)
    private Long shippingFeeVnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_PAYMENT_METHOD_ID, nullable = false)
    private PaymentMethodEntity paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_USER_ID, nullable = false)
    private UserEntity user;

    @Column(name = COL_CHECKOUT_SESSION_PUBLIC_ID, length = 128, unique = true)
    private String checkoutSessionPublicId;

    @Column(name = COL_VNPAY_CHECKOUT_TXN_REF, unique = true)
    private Long vnpayCheckoutTxnRef;
}
