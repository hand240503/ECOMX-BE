package com.ndh.ShopTechnology.services.order;

import com.ndh.ShopTechnology.dto.request.order.CheckoutPricingPreviewRequest;
import com.ndh.ShopTechnology.dto.request.order.CreateOrderRequest;
import com.ndh.ShopTechnology.dto.request.order.OrderReturnRequest;
import com.ndh.ShopTechnology.dto.response.order.CheckoutPricingPreviewResponse;
import com.ndh.ShopTechnology.dto.response.order.CreateOrderResultResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderResponse;
import com.ndh.ShopTechnology.dto.response.order.VnpayPendingTransactionResponse;
import com.ndh.ShopTechnology.dto.response.order.VnpayTransactionStatusResponse;

import java.util.List;

public interface OrderService {

    CheckoutPricingPreviewResponse previewCheckoutPricing(CheckoutPricingPreviewRequest request);

    CreateOrderResultResponse createOrder(CreateOrderRequest request);

    List<OrderResponse> getMyOrders(Integer status);

    OrderResponse getMyOrderById(Long id);

    OrderResponse requestReturn(Long orderId, OrderReturnRequest request);

    OrderResponse cancelMyOrder(Long orderId);

    OrderResponse confirmPayment(Long orderId);

    VnpaySessionFinalizeResult finalizeVnpayCheckoutSession(long checkoutSessionId);

    VnpayPendingTransactionResponse getVnpayPendingTransactionByPublicId(String publicId);

    VnpayTransactionStatusResponse getVnpayTransactionStatusByPublicId(String publicId);

    void abandonVnpayCheckoutSessionByPublicId(String publicId);

    OrderResponse devSimulateVnpayPaymentSuccess(String transactionPublicId);

    List<OrderResponse> adminGetAllOrders(Integer status);

    OrderResponse adminGetOrderById(Long orderId);

    OrderResponse adminUpdateOrderStatus(Long orderId, Integer newStatus);
}
