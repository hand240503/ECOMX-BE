package com.ndh.ShopTechnology.entities.notification;

import com.ndh.ShopTechnology.entities.BaseEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

/** Thông báo gửi tới 1 người dùng (cập nhật đơn hàng, trả hàng, thanh toán...). */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "ix_notification_user", columnList = "user_id"),
        @Index(name = "ix_notification_is_read", columnList = "is_read")
})
public class NotificationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", length = 1000)
    private String message;

    /** ORDER_STATUS | RETURN_REFUND | PAYMENT */
    @Column(name = "type", length = 32)
    private String type;

    /** Đơn hàng liên quan (để bấm vào điều hướng). Có thể null. */
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;
}
