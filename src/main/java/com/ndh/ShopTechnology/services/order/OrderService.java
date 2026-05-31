package com.ndh.ShopTechnology.services.order;

import com.ndh.ShopTechnology.dto.request.order.CheckoutPricingPreviewRequest;
import com.ndh.ShopTechnology.dto.request.order.CreateOrderRequest;
import com.ndh.ShopTechnology.dto.request.order.OrderReturnRequest;
import com.ndh.ShopTechnology.dto.response.order.CheckoutPricingPreviewResponse;
import com.ndh.ShopTechnology.dto.response.order.CreateOrderResultResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderTimelineResponse;
import com.ndh.ShopTechnology.dto.response.order.VnpayPendingTransactionResponse;
import com.ndh.ShopTechnology.dto.response.order.VnpayTransactionStatusResponse;

import java.util.List;

public interface OrderService {

    CheckoutPricingPreviewResponse previewCheckoutPricing(CheckoutPricingPreviewRequest request);

    CreateOrderResultResponse createOrder(CreateOrderRequest request);

    List<OrderResponse> getMyOrders(Integer status);

    OrderResponse getMyOrderById(Long id);

    /**
     * Lấy tiến trình đơn hàng dưới dạng danh sách bước (stepper) cho user FE.
     * Chỉ trả về nếu đơn hàng thuộc về user đang đăng nhập.
     *
     * @param orderId ID đơn hàng
     * @return {@link OrderTimelineResponse} với các bước và trạng thái từng bước
     */
    OrderTimelineResponse getMyOrderTimeline(Long orderId);

    OrderResponse requestReturn(Long orderId, OrderReturnRequest request);

    OrderResponse cancelMyOrder(Long orderId, String cancelReason);

    OrderResponse confirmPayment(Long orderId);

    VnpaySessionFinalizeResult finalizeVnpayCheckoutSession(long checkoutSessionId);

    VnpayPendingTransactionResponse getVnpayPendingTransactionByPublicId(String publicId);

    VnpayTransactionStatusResponse getVnpayTransactionStatusByPublicId(String publicId);

    void abandonVnpayCheckoutSessionByPublicId(String publicId);

    OrderResponse devSimulateVnpayPaymentSuccess(String transactionPublicId);

    List<OrderResponse> adminGetAllOrders(Integer status);

    OrderResponse adminGetOrderById(Long orderId);

    OrderResponse adminUpdateOrderStatus(Long orderId, Integer newStatus, String cancelNote);

    OrderResponse adminUpdateReturnStatus(Long orderId, Integer newReturnStatus, String note);
}
