package com.ndh.ShopTechnology.entities.order;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "checkout_sessions", indexes = {
        @Index(name = "ix_checkout_user", columnList = "user_id"),
        @Index(name = "ix_checkout_status", columnList = "status"),
        @Index(name = "ix_checkout_public_id", columnList = "public_id")
})
public class CheckoutSessionEntity extends BaseEntity {

    /**
     * UUID bảo mật cho FE tra cứu trạng thái phiên (thay vì lộ id số nội bộ). Khác {@code vnp_TxnRef} (id phiên) gửi VNPAY.
     */
    @Column(name = "public_id", nullable = false, unique = true, length = 36, updatable = false)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethodEntity paymentMethod;

    @Column(name = "total", nullable = false)
    private Double total;

    /** JSON {@link com.ndh.ShopTechnology.dto.request.order.CreateOrderRequest} tại thời điểm checkout. */
    @Column(name = "request_payload_json", nullable = false, columnDefinition = "TEXT")
    private String requestPayloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private CheckoutSessionStatus status = CheckoutSessionStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Date expiresAt;

    /**
     * Khi {@link #status} là {@code COMPLETED} — liên kết tới bảng {@code orders}; bản ghi phiên vẫn giữ lịch sử, không
     * xóa dòng.
     */
    @Column(name = "order_id", nullable = true)
    private Long orderId;

    @PrePersist
    void ensurePublicId() {
        if (publicId == null || publicId.isEmpty()) {
            publicId = java.util.UUID.randomUUID().toString();
        }
    }
}
