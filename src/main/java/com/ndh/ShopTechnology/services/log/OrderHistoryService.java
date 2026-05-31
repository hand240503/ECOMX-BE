package com.ndh.ShopTechnology.services.log;

import com.ndh.ShopTechnology.entities.log.OrderHistoryEntity;
import com.ndh.ShopTechnology.entities.order.OrderEntity;

import java.util.List;

public interface OrderHistoryService {

    /**
     * Ghi lịch sử khi trạng thái đơn hàng (status) thay đổi.
     */
    void logOrderStatusChange(OrderEntity order, Integer oldStatus, Integer newStatus, String note);

    /**
     * Ghi lịch sử khi trạng thái trả hàng / hoàn tiền thay đổi.
     */
    void logReturnRefundStatusChange(OrderEntity order,
                                     Integer oldReturnRefundStatus,
                                     Integer newReturnRefundStatus,
                                     String note);

    /**
     * Lấy toàn bộ lịch sử của một đơn hàng.
     */
    List<OrderHistoryEntity> getHistory(Long orderId);

    /**
     * Lấy lịch sử thay đổi trạng thái đơn hàng (không bao gồm return/refund).
     */
    List<OrderHistoryEntity> getOrderStatusHistory(Long orderId);

    /**
     * Lấy lịch sử thay đổi trạng thái trả hàng / hoàn tiền.
     */
    List<OrderHistoryEntity> getReturnRefundHistory(Long orderId);
}
