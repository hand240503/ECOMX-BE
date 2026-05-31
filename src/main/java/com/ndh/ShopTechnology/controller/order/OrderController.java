package com.ndh.ShopTechnology.controller.order;

import com.ndh.ShopTechnology.dto.request.order.CancelOrderRequest;
import com.ndh.ShopTechnology.dto.request.order.CheckoutPricingPreviewRequest;
import com.ndh.ShopTechnology.dto.request.order.CreateOrderRequest;
import com.ndh.ShopTechnology.dto.request.order.OrderReturnRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.order.CheckoutPricingPreviewResponse;
import com.ndh.ShopTechnology.dto.response.order.CreateOrderResultResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderTimelineResponse;
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
     * Lấy tiến trình đơn hàng dưới dạng stepper để hiển thị trên user FE.
     *
     * <p>Ví dụ response (đơn đang ở bước "Đã giao cho ĐVVC"):
     * <pre>
     * GET /orders/42/timeline
     * {
     *   "orderCode": "ORD-2026-0042",
     *   "currentStatus": 3,
     *   "currentStatusLabel": "Đang giao hàng",
     *   "finished": false,
     *   "steps": [
     *     { "stepIndex": 1, "statusCode": 1, "statusLabel": "Đơn hàng đã đặt",
     *       "completed": true, "current": false, "timestamp": "2026-05-25T07:25:00" },
     *     { "stepIndex": 2, "statusCode": 2, "statusLabel": "Đã xác nhận",
     *       "completed": true, "current": false, "timestamp": "2026-05-25T07:30:00",
     *       "updatedByUserId": 5, "updatedByUsername": "employee01",
     *       "updatedByFullName": "Nguyễn Văn A" },
     *     { "stepIndex": 3, "statusCode": 3, "statusLabel": "Đã giao cho ĐVVC",
     *       "completed": true, "current": true, "timestamp": "2026-05-25T17:27:00",
     *       "updatedByUserId": 5, "updatedByUsername": "employee01",
     *       "updatedByFullName": "Nguyễn Văn A" },
     *     { "stepIndex": 4, "statusCode": 4, "statusLabel": "Hoàn thành",
     *       "completed": false, "current": false },
     *     { "stepIndex": 5, "statusLabel": "Đánh giá",
     *       "completed": false, "current": false }
     *   ]
     * }
     * </pre>
     */
    @GetMapping("/{id}/timeline")
    public ResponseEntity<APIResponse<OrderTimelineResponse>> getMyOrderTimeline(
            @PathVariable Long id) {
        OrderTimelineResponse data = orderService.getMyOrderTimeline(id);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }

    @GetMapping("/vnpay-pending/{transactionPublicId}")
    public ResponseEntity<APIResponse<VnpayPendingTransactionResponse>> getVnpayPendingTransaction(
            @PathVariable String transactionPublicId) {
        VnpayPendingTransactionResponse data = orderService.getVnpayPendingTransactionByPublicId(transactionPublicId);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }

    @GetMapping("/vnpay-pending/{transactionPublicId}/transaction-status")
    public ResponseEntity<APIResponse<VnpayTransactionStatusResponse>> getVnpayTransactionStatus(
            @PathVariable String transactionPublicId) {
        VnpayTransactionStatusResponse data =
                orderService.getVnpayTransactionStatusByPublicId(transactionPublicId);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }

    @PostMapping("/vnpay-pending/{transactionPublicId}/abandon")
    public ResponseEntity<APIResponse<Void>> abandonVnpayPending(
            @PathVariable String transactionPublicId) {
        orderService.abandonVnpayCheckoutSessionByPublicId(transactionPublicId);
        return ResponseEntity.ok(APIResponse.of(true, "Checkout session cancelled", null, null, null));
    }

    @PostMapping("/vnpay-pending/{transactionPublicId}/dev-simulate-success")
    public ResponseEntity<APIResponse<OrderResponse>> devSimulateVnpaySuccess(
            @PathVariable String transactionPublicId) {
        OrderResponse data = orderService.devSimulateVnpayPaymentSuccess(transactionPublicId);
        return ResponseEntity.ok(APIResponse.of(true, "Dev: VNPAY success simulated; order created", data, null, null));
    }

    @PostMapping("/checkout-pricing-preview")
    public ResponseEntity<APIResponse<CheckoutPricingPreviewResponse>> previewCheckoutPricing(
            @Valid @RequestBody CheckoutPricingPreviewRequest request) {
        CheckoutPricingPreviewResponse data = orderService.previewCheckoutPricing(request);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
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
    public ResponseEntity<APIResponse<OrderResponse>> cancelMyOrder(
            @PathVariable Long id,
            @Valid @RequestBody CancelOrderRequest request) {
        OrderResponse data = orderService.cancelMyOrder(id, request.getReason());
        return ResponseEntity.ok(APIResponse.of(true, "Order cancelled", data, null, null));
    }

    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<APIResponse<OrderResponse>> confirmPayment(@PathVariable Long id) {
        OrderResponse data = orderService.confirmPayment(id);
        return ResponseEntity.ok(APIResponse.of(true, "Payment recorded", data, null, null));
    }
}
