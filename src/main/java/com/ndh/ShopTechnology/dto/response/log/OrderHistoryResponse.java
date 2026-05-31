package com.ndh.ShopTechnology.dto.response.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndh.ShopTechnology.entities.log.OrderHistoryEntity;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderHistoryResponse {

    private Long id;
    private Long orderId;
    private String orderCode;
    private String changeType;

    // ORDER_STATUS
    private Integer oldStatus;
    private Integer newStatus;
    private String oldStatusLabel;
    private String newStatusLabel;

    // RETURN_REFUND_STATUS
    private Integer oldReturnRefundStatus;
    private Integer newReturnRefundStatus;
    private String oldReturnRefundStatusLabel;
    private String newReturnRefundStatusLabel;

    private String note;
    private Long changedByUserId;
    private String changedByUsername;
    /** Họ tên đầy đủ của người thực hiện (từ user_info.full_name) */
    private String changedByFullName;
    private Date createdAt;

    public static OrderHistoryResponse fromEntity(OrderHistoryEntity e) {
        if (e == null) return null;

        String fullName = null;
        if (e.getChangedByUser() != null && e.getChangedByUser().getUserInfo() != null) {
            fullName = e.getChangedByUser().getUserInfo().getFullName();
        }

        return OrderHistoryResponse.builder()
                .id(e.getId())
                .orderId(e.getOrder() != null ? e.getOrder().getId() : null)
                .orderCode(e.getOrder() != null ? e.getOrder().getOrderCode() : null)
                .changeType(e.getChangeType())
                .oldStatus(e.getOldStatus())
                .newStatus(e.getNewStatus())
                .oldStatusLabel(orderStatusLabel(e.getOldStatus()))
                .newStatusLabel(orderStatusLabel(e.getNewStatus()))
                .oldReturnRefundStatus(e.getOldReturnRefundStatus())
                .newReturnRefundStatus(e.getNewReturnRefundStatus())
                .oldReturnRefundStatusLabel(returnRefundStatusLabel(e.getOldReturnRefundStatus()))
                .newReturnRefundStatusLabel(returnRefundStatusLabel(e.getNewReturnRefundStatus()))
                .note(e.getNote())
                .changedByUserId(e.getChangedByUser() != null ? e.getChangedByUser().getId() : null)
                .changedByUsername(e.getChangedByUsername())
                .changedByFullName(fullName)
                .createdAt(e.getCreatedAt())
                .build();
    }

    private static String orderStatusLabel(Integer status) {
        if (status == null) return null;
        return switch (status) {
            case 1 -> "Chờ xác nhận";
            case 2 -> "Chờ lấy hàng";
            case 3 -> "Đang giao hàng";
            case 4 -> "Hoàn thành";
            case 5 -> "Đã hủy";
            default -> "Không xác định (" + status + ")";
        };
    }

    private static String returnRefundStatusLabel(Integer status) {
        if (status == null) return null;
        return switch (status) {
            case 0 -> "Không có yêu cầu";
            case 1 -> "Đã yêu cầu";
            case 2 -> "Đã chấp nhận";
            case 3 -> "Đang hoàn tiền";
            case 4 -> "Đã hoàn tiền";
            case 5 -> "Đã từ chối";
            default -> "Không xác định (" + status + ")";
        };
    }
}
