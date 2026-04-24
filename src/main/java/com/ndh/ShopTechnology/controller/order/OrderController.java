package com.ndh.ShopTechnology.controller.order;

import com.ndh.ShopTechnology.dto.request.order.CreateOrderRequest;
import com.ndh.ShopTechnology.dto.request.order.OrderReturnRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.order.CreateOrderResultResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderResponse;
import com.ndh.ShopTechnology.dto.response.order.VnpayPendingTransactionResponse;
import com.ndh.ShopTechnology.dto.response.order.VnpayTransactionStatusResponse;
import com.ndh.ShopTechnology.services.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<OrderResponse>>> listMyOrders(
            @RequestParam(required = false) Integer status) {
        List<OrderResponse> data = orderService.getMyOrders(status);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<OrderResponse>> getMyOrder(@PathVariable Long id) {
        OrderResponse data = orderService.getMyOrderById(id);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }

    /**
     * Trạng thái thanh toán VNPAY theo {@code transactionPublicId} từ body tạo đơn (PENDING, EXPIRED, FAILED, CANCELLED,
     * hoặc COMPLETED kèm đơn).
     */
    @GetMapping("/vnpay-pending/{transactionPublicId}")
    public ResponseEntity<APIResponse<VnpayPendingTransactionResponse>> getVnpayPendingTransaction(
            @PathVariable String transactionPublicId) {
        VnpayPendingTransactionResponse data = orderService.getVnpayPendingTransactionByPublicId(transactionPublicId);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }

    /**
     * Mã thống kê tương đương {@code vnp_TransactionStatus} (VNPAY): PENDING / chờ thanh toán → {@code 01} "Giao dịch
     * chưa hoàn tất", thành công → {@code 00}, thất bại/hủy theo từng trường hợp.
     */
    @GetMapping("/vnpay-pending/{transactionPublicId}/transaction-status")
    public ResponseEntity<APIResponse<VnpayTransactionStatusResponse>> getVnpayTransactionStatus(
            @PathVariable String transactionPublicId) {
        VnpayTransactionStatusResponse data =
                orderService.getVnpayTransactionStatusByPublicId(transactionPublicId);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }

    /**
     * Hủy phiên VNPAY PENDING: cập nhật {@code checkout_sessions} sang CANCELLED (giữ bản ghi lịch sử). Dùng khi KH
     * bấm hủy / rời trang thanh toán, hoặc mở khóa tồn theo phiên nếu có.
     */
    @PostMapping("/vnpay-pending/{transactionPublicId}/abandon")
    public ResponseEntity<APIResponse<Void>> abandonVnpayPending(
            @PathVariable String transactionPublicId) {
        orderService.abandonVnpayCheckoutSessionByPublicId(transactionPublicId);
        return ResponseEntity.ok(APIResponse.of(true, "Checkout session cancelled", null, null, null));
    }

    /**
     * [DEV] Mô phỏng IPN thành công: tạo đơn + cập nhật phiên COMPLETED. Bật bằng
     * {@code vnpay.dev-simulate-success-enabled=true}. Dùng khi localhost không nhận IPN; không bật production.
     */
    @PostMapping("/vnpay-pending/{transactionPublicId}/dev-simulate-success")
    public ResponseEntity<APIResponse<OrderResponse>> devSimulateVnpaySuccess(
            @PathVariable String transactionPublicId) {
        OrderResponse data = orderService.devSimulateVnpayPaymentSuccess(transactionPublicId);
        return ResponseEntity.ok(APIResponse.of(true, "Dev: VNPAY success simulated; order created", data, null, null));
    }

    @PostMapping
    public ResponseEntity<APIResponse<CreateOrderResultResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResultResponse data = orderService.createOrder(request);
        String msg = "PENDING_VNPAY_PAYMENT".equals(data.getOutcome())
                ? data.getMessage() != null ? data.getMessage() : "Awaiting VNPAY payment"
                : "Order created successfully";
        APIResponse<CreateOrderResultResponse> response = APIResponse.of(
                true,
                msg,
                data,
                null,
                null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/return-request")
    public ResponseEntity<APIResponse<OrderResponse>> requestReturn(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) OrderReturnRequest request) {
        OrderReturnRequest body = request != null ? request : new OrderReturnRequest();
        OrderResponse data = orderService.requestReturn(id, body);
        return ResponseEntity.ok(APIResponse.of(true, "Return request submitted", data, null, null));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<APIResponse<OrderResponse>> cancelMyOrder(@PathVariable Long id) {
        OrderResponse data = orderService.cancelMyOrder(id);
        return ResponseEntity.ok(APIResponse.of(true, "Order cancelled", data, null, null));
    }

    /**
     * Ghi nhận thanh toán thành công (FE / callback giả lập). Mốc {@code paidAt} dùng tính hạn 7 ngày trả/hoàn.
     */
    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<APIResponse<OrderResponse>> confirmPayment(@PathVariable Long id) {
        OrderResponse data = orderService.confirmPayment(id);
        return ResponseEntity.ok(APIResponse.of(true, "Payment recorded", data, null, null));
    }
}
