package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.order.AdminUpdateOrderStatusRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderResponse;
import com.ndh.ShopTechnology.services.order.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API quản lý đơn hàng dành cho admin/staff.
 *
 * <p>Tất cả endpoint nằm dưới {@code /admin/**} — đã lọc role qua {@link com.ndh.ShopTechnology.config.WebSecurityConfig}.
 * {@code @PreAuthorize} kiểm tra thêm permission code cụ thể.
 *
 * <p>Phân quyền:
 * <ul>
 *   <li>{@code GET} — {@link PermissionCode#READ_ORDER} (500002)</li>
 *   <li>{@code PATCH status} — {@link PermissionCode#UPDATE_ORDER} (500003)</li>
 * </ul>
 */
@RestController
@RequestMapping("${api.prefix}/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * Lấy tất cả đơn hàng, tuỳ chọn lọc theo {@code status} (1–5).
     */
    @GetMapping
    @PreAuthorize("@perm.check(" + PermissionCode.READ_ORDER + ")")
    public ResponseEntity<APIResponse<List<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) Integer status) {
        List<OrderResponse> orders = orderService.adminGetAllOrders(status);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Lấy danh sách đơn hàng thành công",
                orders,
                null,
                Map.of("count", orders.size())));
    }

    /**
     * Lấy chi tiết đơn hàng bất kỳ.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@perm.check(" + PermissionCode.READ_ORDER + ")")
    public ResponseEntity<APIResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse order = orderService.adminGetOrderById(id);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Lấy chi tiết đơn hàng thành công",
                order,
                null,
                null));
    }

    /**
     * Cập nhật trạng thái đơn hàng (1–5, không bao gồm returnRefundStatus).
     * <p>Luồng chuyển hợp lệ: 1→2/5, 2→3/5, 3→4/5. Trạng thái 4 và 5 là khoá cuối — không chỉnh được.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_ORDER + ")")
    public ResponseEntity<APIResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateOrderStatusRequest request) {
        OrderResponse order = orderService.adminUpdateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Cập nhật trạng thái đơn hàng thành công",
                order,
                null,
                null));
    }
}
