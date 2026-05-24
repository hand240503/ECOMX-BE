package com.ndh.ShopTechnology.dto.request.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body của {@code PATCH /admin/orders/{id}/status}.
 * <p>
 * Trạng thái hợp lệ (trừ returnRefundStatus — quản lý riêng):
 * <ul>
 *   <li>1 — Chờ chuẩn bị</li>
 *   <li>2 — Chờ vận chuyển</li>
 *   <li>3 — Chờ giao hàng</li>
 *   <li>4 — Hoàn thành</li>
 *   <li>5 — Đã hủy</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateOrderStatusRequest {

    @NotNull(message = "Trạng thái đơn hàng không được để trống")
    @Min(value = 1, message = "Trạng thái phải từ 1 đến 5")
    @Max(value = 5, message = "Trạng thái phải từ 1 đến 5")
    private Integer status;
}
