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

    /**
     * Preview giá checkout: cùng engine với tạo đơn (PC, volume tier, PWP) để FE hiển thị trước khi POST đặt hàng.
     */
    CheckoutPricingPreviewResponse previewCheckoutPricing(CheckoutPricingPreviewRequest request);

    CreateOrderResultResponse createOrder(CreateOrderRequest request);

    List<OrderResponse> getMyOrders(Integer status);

    OrderResponse getMyOrderById(Long id);

    OrderResponse requestReturn(Long orderId, OrderReturnRequest request);

    /** Chỉ khi status = 1 (chờ xác nhận) và chưa ghi nhận thanh toán. */
    OrderResponse cancelMyOrder(Long orderId);

    /** Ghi nhận thanh toán thành công (mốc tính 7 ngày trả/hoàn) — tích hợp gateway sau này. */
    OrderResponse confirmPayment(Long orderId);

    /** Tạo đơn từ payload phiên VNPAY sau khi VNPAY IPN báo thành công (gọi trong luồng thanh toán). */
    VnpaySessionFinalizeResult finalizeVnpayCheckoutSession(long checkoutSessionId);

    /**
     * Trạng thái phiên VNPAY theo UUID từ {@link CreateOrderResultResponse#getTransactionPublicId()}. Khi thành công,
     * phiên vẫn tồn tại ở trạng thái COMPLETED kèm {@code orderId}; cũng có thể tìm đơn theo
     * {@code orders.checkoutSessionPublicId}.
     */
    VnpayPendingTransactionResponse getVnpayPendingTransactionByPublicId(String publicId);

    /**
     * Trả về mã thống kê tương đương {@code vnp_TransactionStatus} (VNPAY) và mô tả: ví dụ PENDING → "01" chưa hoàn
     * tất, thành công → "00".
     */
    VnpayTransactionStatusResponse getVnpayTransactionStatusByPublicId(String publicId);

    /**
     * Hủy phiên PENDING: ghi nhận {@code CANCELLED} (không xóa bản ghi). Idempotent nếu đã CANCELLED. Trước khi cập nhật
     * gọi hook giải phóng lock kho nếu có.
     */
    void abandonVnpayCheckoutSessionByPublicId(String publicId);

    /**
     * [DEV] Mô phỏng IPN thành công: tạo order và chuyển phiên {@code COMPLETED}. Chỉ khi
     * {@code vnpay.dev-simulate-success-enabled=true}. Idempotent nếu đã {@code COMPLETED}.
     */
    OrderResponse devSimulateVnpayPaymentSuccess(String transactionPublicId);

    // =====================================================================
    // ADMIN
    // =====================================================================

    /**
     * Admin: lấy tất cả đơn hàng, tuỳ chọn lọc theo {@code status} (1–5).
     * Yêu cầu quyền {@link com.ndh.ShopTechnology.constants.PermissionCode#READ_ORDER}.
     */
    List<OrderResponse> adminGetAllOrders(Integer status);

    /**
     * Admin: lấy chi tiết đơn hàng bất kỳ theo id.
     * Yêu cầu quyền {@link com.ndh.ShopTechnology.constants.PermissionCode#READ_ORDER}.
     */
    OrderResponse adminGetOrderById(Long orderId);

    /**
     * Admin: cập nhật trạng thái đơn hàng (status 1–5, không bao gồm returnRefundStatus).
     * <p>Quy tắc chuyển trạng thái:
     * <ul>
     *   <li>1 → 2, 5 (xác nhận hoặc hủy)</li>
     *   <li>2 → 3, 5 (bàn giao VC hoặc hủy)</li>
     *   <li>3 → 4, 5 (giao thành công hoặc hủy)</li>
     *   <li>4 → không thay đổi (hoàn thành — khoá)</li>
     *   <li>5 → không thay đổi (đã hủy — khoá)</li>
     * </ul>
     * Yêu cầu quyền {@link com.ndh.ShopTechnology.constants.PermissionCode#UPDATE_ORDER}.
     */
    OrderResponse adminUpdateOrderStatus(Long orderId, Integer newStatus);
}
