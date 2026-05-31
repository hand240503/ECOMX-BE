package com.ndh.ShopTechnology.dto.response.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndh.ShopTechnology.entities.log.AdminActivityLogEntity;
import com.ndh.ShopTechnology.entities.log.OrderHistoryEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * Response thống nhất cho cả hai nguồn lịch sử:
 * <ul>
 *   <li>{@code ORDER_HISTORY}  – từ bảng {@code order_history}</li>
 *   <li>{@code ACTIVITY_LOG}   – từ bảng {@code admin_activity_log}</li>
 * </ul>
 *
 * <p>Các trường chỉ có ở một nguồn sẽ là {@code null} ở nguồn kia.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnifiedHistoryResponse {

    // ── nguồn gốc ─────────────────────────────────────────────────────────────
    /** ORDER_HISTORY | ACTIVITY_LOG */
    private String source;

    private Long id;
    private Date createdAt;

    // ── ai thực hiện ──────────────────────────────────────────────────────────
    private Long    actorUserId;
    private String  actorUsername;
    /** Họ tên đầy đủ của người thực hiện (từ user_info.full_name) */
    private String  actorFullName;

    /** Chỉ có ở ACTIVITY_LOG */
    private String  ipAddress;

    // ── hành động ────────────────────────────────────────────────────────────
    /**
     * ACTIVITY_LOG : CREATE | UPDATE | DELETE
     * ORDER_HISTORY: ORDER_STATUS | RETURN_REFUND_STATUS
     */
    private String action;

    // ── entity bị tác động ───────────────────────────────────────────────────
    /**
     * ACTIVITY_LOG : PRODUCT | BRAND | CATEGORY | PRICE_CHANGE | VOLUME_TIER | PWP_OFFER
     * ORDER_HISTORY: ORDER
     */
    private String  entityType;
    private Long    entityId;
    /** Tên / mã đơn hàng hoặc tên entity */
    private String  entityLabel;

    // ── chi tiết ACTIVITY_LOG ────────────────────────────────────────────────
    /** JSON trạng thái trước (chỉ có ở ACTIVITY_LOG, UPDATE / DELETE) */
    private String snapshotBefore;
    /** JSON trạng thái sau  (chỉ có ở ACTIVITY_LOG, CREATE / UPDATE) */
    private String snapshotAfter;

    // ── chi tiết ORDER_HISTORY ───────────────────────────────────────────────
    private Integer oldStatus;
    private Integer newStatus;
    private String  oldStatusLabel;
    private String  newStatusLabel;

    private Integer oldReturnRefundStatus;
    private Integer newReturnRefundStatus;
    private String  oldReturnRefundStatusLabel;
    private String  newReturnRefundStatusLabel;

    /** Ghi chú / lý do thay đổi (ORDER_HISTORY) */
    private String note;

    // ── factory methods ───────────────────────────────────────────────────────

    public static UnifiedHistoryResponse fromOrderHistory(OrderHistoryEntity e) {
        if (e == null) return null;
        String fullName = null;
        if (e.getChangedByUser() != null && e.getChangedByUser().getUserInfo() != null) {
            fullName = e.getChangedByUser().getUserInfo().getFullName();
        }
        return UnifiedHistoryResponse.builder()
                .source("ORDER_HISTORY")
                .id(e.getId())
                .createdAt(e.getCreatedAt())
                .actorUserId(e.getChangedByUser() != null ? e.getChangedByUser().getId() : null)
                .actorUsername(e.getChangedByUsername())
                .actorFullName(fullName)
                .action(e.getChangeType())                           // ORDER_STATUS / RETURN_REFUND_STATUS
                .entityType("ORDER")
                .entityId(e.getOrder() != null ? e.getOrder().getId() : null)
                .entityLabel(e.getOrder() != null ? e.getOrder().getOrderCode() : null)
                .oldStatus(e.getOldStatus())
                .newStatus(e.getNewStatus())
                .oldStatusLabel(orderStatusLabel(e.getOldStatus()))
                .newStatusLabel(orderStatusLabel(e.getNewStatus()))
                .oldReturnRefundStatus(e.getOldReturnRefundStatus())
                .newReturnRefundStatus(e.getNewReturnRefundStatus())
                .oldReturnRefundStatusLabel(returnRefundStatusLabel(e.getOldReturnRefundStatus()))
                .newReturnRefundStatusLabel(returnRefundStatusLabel(e.getNewReturnRefundStatus()))
                .note(e.getNote())
                .build();
    }

    public static UnifiedHistoryResponse fromActivityLog(AdminActivityLogEntity e) {
        if (e == null) return null;
        String fullName = null;
        if (e.getActorUser() != null && e.getActorUser().getUserInfo() != null) {
            fullName = e.getActorUser().getUserInfo().getFullName();
        }
        return UnifiedHistoryResponse.builder()
                .source("ACTIVITY_LOG")
                .id(e.getId())
                .createdAt(e.getCreatedAt())
                .actorUserId(e.getActorUser() != null ? e.getActorUser().getId() : null)
                .actorUsername(e.getActorUsername())
                .actorFullName(fullName)
                .ipAddress(e.getIpAddress())
                .action(e.getAction())                               // CREATE / UPDATE / DELETE
                .entityType(e.getEntityType())
                .entityId(e.getEntityId())
                .entityLabel(e.getEntityLabel())
                .snapshotBefore(e.getSnapshotBefore())
                .snapshotAfter(e.getSnapshotAfter())
                .build();
    }

    // ── label helpers ─────────────────────────────────────────────────────────

    private static String orderStatusLabel(Integer s) {
        if (s == null) return null;
        return switch (s) {
            case 1 -> "Chờ xác nhận";
            case 2 -> "Chờ lấy hàng";
            case 3 -> "Đang giao hàng";
            case 4 -> "Hoàn thành";
            case 5 -> "Đã hủy";
            default -> "Không xác định (" + s + ")";
        };
    }

    private static String returnRefundStatusLabel(Integer s) {
        if (s == null) return null;
        return switch (s) {
            case 0 -> "Không có yêu cầu";
            case 1 -> "Đã yêu cầu";
            case 2 -> "Đã chấp nhận";
            case 3 -> "Đang hoàn tiền";
            case 4 -> "Đã hoàn tiền";
            case 5 -> "Đã từ chối";
            default -> "Không xác định (" + s + ")";
        };
    }
}
