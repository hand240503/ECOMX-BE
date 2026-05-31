package com.ndh.ShopTechnology.entities.log;

import com.ndh.ShopTechnology.entities.order.OrderEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

/**
 * Lưu lại mỗi lần trạng thái đơn hàng (status) hoặc trạng thái trả hàng/hoàn tiền
 * (return_refund_status) thay đổi.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "order_history", indexes = {
        @Index(name = "idx_order_history_order_id", columnList = "order_id"),
        @Index(name = "idx_order_history_changed_by", columnList = "changed_by_user_id")
})
public class OrderHistoryEntity {

    public static final String COL_ID                        = "id";
    public static final String COL_ORDER_ID                  = "order_id";
    public static final String COL_CHANGE_TYPE               = "change_type";
    public static final String COL_OLD_STATUS                = "old_status";
    public static final String COL_NEW_STATUS                = "new_status";
    public static final String COL_OLD_RETURN_REFUND_STATUS  = "old_return_refund_status";
    public static final String COL_NEW_RETURN_REFUND_STATUS  = "new_return_refund_status";
    public static final String COL_NOTE                      = "note";
    public static final String COL_CHANGED_BY_USER_ID        = "changed_by_user_id";
    public static final String COL_CHANGED_BY_USERNAME       = "changed_by_username";
    public static final String COL_CREATED_AT                = "created_at";

    /**
     * Loại thay đổi:
     * "ORDER_STATUS"        – thay đổi trạng thái đơn hàng
     * "RETURN_REFUND_STATUS" – thay đổi trạng thái trả hàng / hoàn tiền
     */
    public static final String CHANGE_TYPE_ORDER_STATUS         = "ORDER_STATUS";
    public static final String CHANGE_TYPE_RETURN_REFUND_STATUS = "RETURN_REFUND_STATUS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = COL_ID)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COL_ORDER_ID, nullable = false)
    private OrderEntity order;

    /** "ORDER_STATUS" hoặc "RETURN_REFUND_STATUS" */
    @Column(name = COL_CHANGE_TYPE, nullable = false, length = 32)
    private String changeType;

    @Column(name = COL_OLD_STATUS)
    private Integer oldStatus;

    @Column(name = COL_NEW_STATUS)
    private Integer newStatus;

    @Column(name = COL_OLD_RETURN_REFUND_STATUS)
    private Integer oldReturnRefundStatus;

    @Column(name = COL_NEW_RETURN_REFUND_STATUS)
    private Integer newReturnRefundStatus;

    @Column(name = COL_NOTE, columnDefinition = "TEXT")
    private String note;

    /** FK đến user đã thực hiện thay đổi (nullable – hệ thống tự thay đổi thì null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = COL_CHANGED_BY_USER_ID)
    private UserEntity changedByUser;

    /** Snapshot username tại thời điểm ghi log (tránh mất dữ liệu nếu user bị xóa) */
    @Column(name = COL_CHANGED_BY_USERNAME, length = 128)
    private String changedByUsername;

    @Column(name = COL_CREATED_AT, nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date createdAt = new Date();
}
