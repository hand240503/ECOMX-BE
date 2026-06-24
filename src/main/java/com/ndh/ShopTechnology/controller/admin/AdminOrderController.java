package com.ndh.ShopTechnology.controller.admin;

import com.ndh.ShopTechnology.constants.PermissionCode;
import com.ndh.ShopTechnology.dto.request.order.AdminUpdateOrderStatusRequest;
import com.ndh.ShopTechnology.dto.request.order.AdminUpdateReturnStatusRequest;
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

@RestController
@RequestMapping("${api.prefix}/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

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

    @PatchMapping("/{id}/status")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_ORDER + ")")
    public ResponseEntity<APIResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateOrderStatusRequest request) {
        OrderResponse order = orderService.adminUpdateOrderStatus(id, request.getStatus(), request.getCancelNote());
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Cập nhật trạng thái đơn hàng thành công",
                order,
                null,
                null));
    }

    @PatchMapping("/{id}/return-status")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_ORDER + ")")
    public ResponseEntity<APIResponse<OrderResponse>> updateReturnStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateReturnStatusRequest request) {
        boolean restockToSellable = request.getRestockToSellable() == null || request.getRestockToSellable();
        OrderResponse order = orderService.adminUpdateReturnStatus(
                id, request.getReturnStatus(), request.getNote(), restockToSellable);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Cập nhật trạng thái trả hàng thành công",
                order,
                null,
                null));
    }

    @DeleteMapping("/{id}/return-media/{mediaId}")
    @PreAuthorize("@perm.check(" + PermissionCode.UPDATE_ORDER + ")")
    public ResponseEntity<APIResponse<OrderResponse>> deleteReturnMedia(
            @PathVariable Long id,
            @PathVariable Long mediaId) {
        OrderResponse order = orderService.adminDeleteReturnMedia(id, mediaId);
        return ResponseEntity.ok(APIResponse.of(
                true,
                "Đã xoá ảnh / video bằng chứng",
                order,
                null,
                null));
    }
}
