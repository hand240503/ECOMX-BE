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

    /**
     * Trả hàng / hoàn tiền: null = chưa có; 1–5 theo {@link com.ndh.ShopTechnology.constants.OrderConstants}.
     * Độc lập với {@link #status} (đơn vẫn giữ 2–4 trong khi xử lý trả).
     */
    @Column(name = COL_RETURN_REFUND_STATUS, nullable = true)
    private Integer returnRefundStatus;

    /** Lý do / ghi chú phía KH lúc gửi yêu cầu trả. */
    @Column(name = COL_RETURN_REFUND_NOTE, columnDefinition = "TEXT")
    private String returnRefundNote;

    @Column(name = COL_PAID, nullable = false)
    @Builder.Default
    private Boolean paid = false;

    /** Thời điểm ghi nhận thanh toán thành công (dùng tính hạn 7 ngày trả/hoàn). */
    @Column(name = COL_PAID_AT, nullable = true)
    private java.util.Date paidAt;

    @Column(name = COL_DESCRIPTION, nullable = true)
    private String description;

    /**
     * Tổng thanh toán: tổng các dòng chi tiết + {@link #shippingFeeVnd} (phí null coi như 0).
     */
    @Column(name = COL_TOTAL, nullable = true)
    private Double total;

    @Column(name = COL_TYPE_ORDER, nullable = true)
    private Integer typeOrder;

    /**
     * Mã hiển thị cho KH/CS (sinh sau khi có id, vd. DH-2026-00000001).
     */
    @Column(name = COL_ORDER_CODE, nullable = true, unique = true, length = 32)
    private String orderCode;

    /**
     * Snapshot địa chỉ giao hàng dạng text (không FK tới bảng user_address).
     */
    @Column(name = COL_DELIVERY_ADDRESS, columnDefinition = "TEXT")
    private String deliveryAddress;

    /** Quãng đường lái tới kho tại thời điểm đặt (OSRM), mét. */
    @Column(name = COL_DELIVERY_DISTANCE_METERS)
    private Double deliveryDistanceMeters;

    /** Phí ship (VND) snapshot theo {@link com.ndh.ShopTechnology.utils.ShippingFeeCalculator}. */
    @Column(name = COL_SHIPPING_FEE_VND)
    private Long shippingFeeVnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_PAYMENT_METHOD_ID, nullable = false)
    private PaymentMethodEntity paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_USER_ID, nullable = false)
    private UserEntity user;

    /**
     * Mã công khai cùng {@link CheckoutSessionEntity#getPublicId()} — đồng bộ với bản ghi phiên; FE/BE đối chiếu
     * {@code GET .../vnpay-pending/{publicId}}.
     */
    @Column(name = COL_CHECKOUT_SESSION_PUBLIC_ID, length = 128, unique = true)
    private String checkoutSessionPublicId;

    /**
     * {@code vnp_TxnRef} tại thời điểm thanh toán (id bảng {@code checkout_sessions}) — idempotency IPN, kể cả dữ liệu
     * cũ khi phiên từng bị xóa sau tạo đơn.
     */
    @Column(name = COL_VNPAY_CHECKOUT_TXN_REF, unique = true)
    private Long vnpayCheckoutTxnRef;
}