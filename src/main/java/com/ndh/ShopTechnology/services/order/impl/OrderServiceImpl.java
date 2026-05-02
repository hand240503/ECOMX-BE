package com.ndh.ShopTechnology.services.order.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndh.ShopTechnology.config.VnpayProperties;
import com.ndh.ShopTechnology.constants.OrderConstants;
import com.ndh.ShopTechnology.constants.SystemConstant;
import com.ndh.ShopTechnology.dto.request.order.CreateOrderDetailRequest;
import com.ndh.ShopTechnology.dto.request.order.CreateOrderHeaderRequest;
import com.ndh.ShopTechnology.dto.request.order.CreateOrderRequest;
import com.ndh.ShopTechnology.dto.request.order.OrderReturnRequest;
import com.ndh.ShopTechnology.dto.response.order.CreateOrderResultResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderDetailResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderResponse;
import com.ndh.ShopTechnology.dto.response.order.PaymentMethodSummaryResponse;
import com.ndh.ShopTechnology.dto.response.order.VnpayCheckoutOrderInfoResponse;
import com.ndh.ShopTechnology.dto.response.order.VnpayPendingTransactionResponse;
import com.ndh.ShopTechnology.dto.response.order.VnpayTransactionStatusResponse;
import com.ndh.ShopTechnology.entities.order.CheckoutSessionEntity;
import com.ndh.ShopTechnology.entities.order.CheckoutSessionStatus;
import com.ndh.ShopTechnology.entities.order.OrderDetailEntity;
import com.ndh.ShopTechnology.entities.order.OrderEntity;
import com.ndh.ShopTechnology.entities.order.PaymentMethodEntity;
import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.user.AddressType;
import com.ndh.ShopTechnology.entities.user.UserAddressEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.CheckoutSessionRepository;
import com.ndh.ShopTechnology.repository.OrderDetailRepository;
import com.ndh.ShopTechnology.repository.OrderRepository;
import com.ndh.ShopTechnology.repository.PaymentMethodRepository;
import com.ndh.ShopTechnology.repository.ProductRepository;
import com.ndh.ShopTechnology.repository.UserAddressRepository;
import com.ndh.ShopTechnology.utils.ShippingFeeCalculator;
import com.ndh.ShopTechnology.services.order.OrderService;
import com.ndh.ShopTechnology.services.order.VnpaySessionFinalizeResult;
import com.ndh.ShopTechnology.services.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final int CHECKOUT_SESSION_VALID_MINUTES = 30;

    /**
     * Bảng mã {@code vnp_TransactionStatus} (tài liệu Thanh toán Pay VNPAY) — bản rút gọn dùng cho API.
     */
    private static final String VNP_TXN_ST_SUCCESS = "00";
    private static final String VNP_TXN_ST_NOT_FINISHED = "01";
    private static final String VNP_TXN_ST_ERROR = "02";
    private static final String VNP_TXN_MSG_SUCCESS = "Giao dịch thành công";
    private static final String VNP_TXN_MSG_NOT_FINISHED = "Giao dịch chưa hoàn tất";
    private static final String VNP_TXN_MSG_ERROR = "Giao dịch bị lỗi";

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final VnpayProperties vnpayProperties;
    private final UserAddressRepository userAddressRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderDetailRepository orderDetailRepository,
            ProductRepository productRepository,
            PaymentMethodRepository paymentMethodRepository,
            CheckoutSessionRepository checkoutSessionRepository,
            UserService userService,
            ObjectMapper objectMapper,
            VnpayProperties vnpayProperties,
            UserAddressRepository userAddressRepository) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.productRepository = productRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.vnpayProperties = vnpayProperties;
        this.userAddressRepository = userAddressRepository;
    }

    @Override
    @Transactional
    public CreateOrderResultResponse createOrder(CreateOrderRequest request) {
        UserEntity currentUser = userService.getCurrentUser();
        if (request.getOrderDetails() == null || request.getOrderDetails().isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "orderDetails must not be empty");
        }
        validateOrderHeaderForShipping(request.getOrder());

        OrderPlaceDraft draft = buildDraft(request);

        String pmCode = draft.paymentMethod().getCode() != null
                ? draft.paymentMethod().getCode().trim()
                : "";

        if (OrderConstants.PM_CODE_COD.equalsIgnoreCase(pmCode)) {
            OrderEntity order = persistOrderFromDraft(draft, currentUser, false, null);
            List<OrderDetailEntity> savedDetails = orderDetailRepository.findByOrder_IdOrderByIdAsc(order.getId());
            OrderResponse orderRes = buildOrderResponse(order, mapDetailResponses(savedDetails));
            return CreateOrderResultResponse.builder()
                    .outcome("ORDER_CREATED")
                    .order(orderRes)
                    .deliveryDistanceMeters(orderRes.getDeliveryDistanceMeters())
                    .shippingFeeVnd(orderRes.getShippingFeeVnd())
                    .message("Order created successfully")
                    .build();
        }

        if (OrderConstants.PM_CODE_VNPAY.equalsIgnoreCase(pmCode)) {
            String json;
            try {
                json = objectMapper.writeValueAsString(request);
            } catch (Exception e) {
                throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not serialize checkout payload");
            }
            Date expiresAt = new Date(System.currentTimeMillis() + CHECKOUT_SESSION_VALID_MINUTES * 60_000L);
            CheckoutShipping ship = resolveCheckoutShipping(request.getOrder(), currentUser.getId(), null);
            double grandTotal = grandOrderTotalVnd(draft.orderTotal(), ship.shippingFeeVnd());
            CreateOrderHeaderRequest header = request.getOrder();
            String workSession = normalizeOptionalCheckoutWorkSessionId(header.getCheckoutWorkSessionId());
            if (workSession != null && checkoutSessionRepository.existsByPublicId(workSession)) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "checkoutWorkSessionId is already in use; choose a new session id");
            }
            CheckoutSessionEntity.CheckoutSessionEntityBuilder sessionBuilder = CheckoutSessionEntity.builder()
                    .user(currentUser)
                    .paymentMethod(draft.paymentMethod())
                    .total(grandTotal)
                    .deliveryDistanceMeters(ship.distanceMeters())
                    .shippingFeeVnd(ship.shippingFeeVnd())
                    .requestPayloadJson(json)
                    .status(CheckoutSessionStatus.PENDING)
                    .expiresAt(expiresAt)
                    .orderId(null);
            if (workSession != null) {
                sessionBuilder.publicId(workSession);
            }
            CheckoutSessionEntity session = sessionBuilder.build();
            session = checkoutSessionRepository.save(session);
            if (workSession == null) {
                session.setPublicId(String.valueOf(session.getId()));
                session = checkoutSessionRepository.save(session);
            }
            PaymentMethodEntity pm = draft.paymentMethod();
            return CreateOrderResultResponse.builder()
                    .outcome("PENDING_VNPAY_PAYMENT")
                    .checkoutSessionId(session.getId())
                    .transactionPublicId(session.getPublicId())
                    .pendingTotal(grandTotal)
                    .deliveryDistanceMeters(ship.distanceMeters())
                    .shippingFeeVnd(ship.shippingFeeVnd())
                    .paymentMethod(PaymentMethodSummaryResponse.builder()
                            .id(pm.getId())
                            .name(pm.getName())
                            .code(pm.getCode())
                            .build())
                    .message("Awaiting VNPAY payment. Call POST .../payment/vnpay/checkout-sessions/{id}/payment-url for the payment URL. "
                            + "Poll GET .../orders/vnpay-pending/{transactionPublicId} for status. "
                            + "The order is created only after successful payment (VNPAY IPN); the checkout session is then set to COMPLETED with order_id.")
                    .build();
        }

        throw new CustomApiException(HttpStatus.BAD_REQUEST,
                "Only COD creates an order immediately. For online payment, only VNPAY is supported for now "
                        + "(the order is created after successful payment). Please choose COD or VNPAY.");
    }

    @Override
    @Transactional
    public VnpaySessionFinalizeResult finalizeVnpayCheckoutSession(long checkoutSessionId) {
        return finalizeVnpayCheckoutSession(checkoutSessionId, false);
    }

    /**
     * @param ignoreExpiry Nếu {@code true} (chỉ luồng dev mô phỏng IPN), bỏ kiểm tra hết hạn phiên; IPN thật luôn dùng
     *                    {@code false}.
     */
    private VnpaySessionFinalizeResult finalizeVnpayCheckoutSession(long checkoutSessionId, boolean ignoreExpiry) {
        CheckoutSessionEntity session = checkoutSessionRepository
                .findByIdWithUserAndPaymentMethod(checkoutSessionId)
                .orElse(null);
        if (session == null) {
            return VnpaySessionFinalizeResult.NOT_FOUND;
        }
        if (session.getStatus() == CheckoutSessionStatus.COMPLETED) {
            return VnpaySessionFinalizeResult.ALREADY_COMPLETED;
        }
        if (session.getStatus() == CheckoutSessionStatus.FAILED
                || session.getStatus() == CheckoutSessionStatus.EXPIRED) {
            return VnpaySessionFinalizeResult.NOT_PAYABLE;
        }
        if (session.getStatus() != CheckoutSessionStatus.PENDING) {
            return VnpaySessionFinalizeResult.NOT_PAYABLE;
        }
        if (!ignoreExpiry
                && session.getExpiresAt() != null
                && session.getExpiresAt().before(new Date())) {
            return VnpaySessionFinalizeResult.NOT_PAYABLE;
        }

        CreateOrderRequest request;
        try {
            request = objectMapper.readValue(session.getRequestPayloadJson(), CreateOrderRequest.class);
        } catch (Exception e) {
            return VnpaySessionFinalizeResult.BUSINESS_ERROR;
        }

        UserEntity user = session.getUser();
        OrderPlaceDraft draft;
        try {
            draft = buildDraft(request);
        } catch (Exception e) {
            return VnpaySessionFinalizeResult.BUSINESS_ERROR;
        }

        CheckoutShipping shipRecalc = resolveCheckoutShipping(request.getOrder(), user.getId(), null);
        double expectedGrand = grandOrderTotalVnd(draft.orderTotal(), shipRecalc.shippingFeeVnd());
        if (Math.abs(expectedGrand - session.getTotal()) > 0.01) {
            return VnpaySessionFinalizeResult.BUSINESS_ERROR;
        }
        String code = draft.paymentMethod().getCode() != null ? draft.paymentMethod().getCode() : "";
        if (!OrderConstants.PM_CODE_VNPAY.equalsIgnoreCase(code)) {
            return VnpaySessionFinalizeResult.BUSINESS_ERROR;
        }

        try {
            OrderEntity order = persistOrderFromDraft(
                    draft, user, true, new Date(), session.getPublicId(), session.getId(), session);
            session.setOrderId(order.getId());
            session.setStatus(CheckoutSessionStatus.COMPLETED);
            checkoutSessionRepository.save(session);
            return VnpaySessionFinalizeResult.CREATED;
        } catch (Exception e) {
            return VnpaySessionFinalizeResult.BUSINESS_ERROR;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VnpayPendingTransactionResponse getVnpayPendingTransactionByPublicId(String publicId) {
        String pub = normalizeCheckoutSessionPublicId(publicId);
        UserEntity currentUser = userService.getCurrentUser();
        long userId = currentUser.getId();

        return checkoutSessionRepository
                .findByPublicIdAndUser_Id(pub, userId)
                .map(session -> toPendingFromSession(session, pub))
                .or(() -> orderRepository
                        .findByCheckoutSessionPublicIdAndUser_Id(pub, userId)
                        .map(order -> toPendingCompleted(pub, order)))
                .orElseThrow(
                        () -> new NotFoundEntityException("VNPAY payment session not found for this id"));
    }

    @Override
    @Transactional(readOnly = true)
    public VnpayTransactionStatusResponse getVnpayTransactionStatusByPublicId(String publicId) {
        String pub = normalizeCheckoutSessionPublicId(publicId);
        UserEntity currentUser = userService.getCurrentUser();
        long userId = currentUser.getId();

        return checkoutSessionRepository
                .findByPublicIdAndUser_Id(pub, userId)
                .map(session -> toVnpayTransactionStatusFromSession(session, pub))
                .or(() -> orderRepository
                        .findByCheckoutSessionPublicIdAndUser_Id(pub, userId)
                        .map(order -> toVnpayTransactionStatusFromCompletedOrder(pub, order.getId())))
                .orElseThrow(
                        () -> new NotFoundEntityException("VNPAY payment session not found for this id"));
    }

    private VnpayTransactionStatusResponse toVnpayTransactionStatusFromCompletedOrder(String publicId, long orderId) {
        return VnpayTransactionStatusResponse.builder()
                .transactionPublicId(publicId)
                .vnpTransactionStatus(VNP_TXN_ST_SUCCESS)
                .vnpTransactionStatusMessage(VNP_TXN_MSG_SUCCESS)
                .internalState("COMPLETED")
                .orderId(orderId)
                .build();
    }

    private VnpayTransactionStatusResponse toVnpayTransactionStatusFromSession(
            CheckoutSessionEntity session, String publicId) {
        String vnp = String.valueOf(session.getId());

        if (session.getStatus() == CheckoutSessionStatus.COMPLETED && session.getOrderId() != null) {
            OrderEntity o = orderRepository
                    .findByIdAndUser_Id(session.getOrderId(), session.getUser().getId())
                    .orElse(null);
            if (o != null) {
                return VnpayTransactionStatusResponse.builder()
                        .transactionPublicId(publicId)
                        .vnpTransactionStatus(VNP_TXN_ST_SUCCESS)
                        .vnpTransactionStatusMessage(VNP_TXN_MSG_SUCCESS)
                        .internalState("COMPLETED")
                        .vnpayTxnRef(vnp)
                        .orderId(o.getId())
                        .build();
            }
        }
        if (session.getStatus() == CheckoutSessionStatus.FAILED) {
            return VnpayTransactionStatusResponse.builder()
                    .transactionPublicId(publicId)
                    .vnpTransactionStatus(VNP_TXN_ST_ERROR)
                    .vnpTransactionStatusMessage(VNP_TXN_MSG_ERROR)
                    .internalState("FAILED")
                    .vnpayTxnRef(vnp)
                    .build();
        }
        if (session.getStatus() == CheckoutSessionStatus.CANCELLED) {
            return VnpayTransactionStatusResponse.builder()
                    .transactionPublicId(publicId)
                    .vnpTransactionStatus(VNP_TXN_ST_ERROR)
                    .vnpTransactionStatusMessage(VNP_TXN_MSG_ERROR)
                    .internalState("CANCELLED")
                    .vnpayTxnRef(vnp)
                    .build();
        }
        if (session.getStatus() == CheckoutSessionStatus.EXPIRED) {
            return VnpayTransactionStatusResponse.builder()
                    .transactionPublicId(publicId)
                    .vnpTransactionStatus(VNP_TXN_ST_NOT_FINISHED)
                    .vnpTransactionStatusMessage(VNP_TXN_MSG_NOT_FINISHED)
                    .internalState("EXPIRED")
                    .vnpayTxnRef(vnp)
                    .build();
        }
        if (session.getStatus() == CheckoutSessionStatus.PENDING) {
            Date now = new Date();
            boolean timeExpired = session.getExpiresAt() != null && session.getExpiresAt().before(now);
            return VnpayTransactionStatusResponse.builder()
                    .transactionPublicId(publicId)
                    .vnpTransactionStatus(VNP_TXN_ST_NOT_FINISHED)
                    .vnpTransactionStatusMessage(VNP_TXN_MSG_NOT_FINISHED)
                    .internalState(timeExpired ? "EXPIRED" : "PENDING")
                    .vnpayTxnRef(vnp)
                    .build();
        }
        return VnpayTransactionStatusResponse.builder()
                .transactionPublicId(publicId)
                .vnpTransactionStatus(VNP_TXN_ST_NOT_FINISHED)
                .vnpTransactionStatusMessage(VNP_TXN_MSG_NOT_FINISHED)
                .internalState(session.getStatus().name())
                .vnpayTxnRef(vnp)
                .build();
    }

    @Override
    @Transactional
    public void abandonVnpayCheckoutSessionByPublicId(String publicId) {
        String pub = normalizeCheckoutSessionPublicId(publicId);
        UserEntity currentUser = userService.getCurrentUser();
        CheckoutSessionEntity session = checkoutSessionRepository
                .findByPublicIdAndUser_Id(pub, currentUser.getId())
                .orElseThrow(() -> new NotFoundEntityException(
                        "Checkout session not found or not owned by current user"));

        if (session.getStatus() == CheckoutSessionStatus.CANCELLED) {
            return;
        }
        if (session.getStatus() == CheckoutSessionStatus.COMPLETED) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Checkout session is already completed; use order APIs if an order exists.");
        }
        if (session.getStatus() != CheckoutSessionStatus.PENDING) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Only a pending session can be cancelled; current status: " + session.getStatus());
        }

        releaseWarehouseLockForAbandonedCheckoutSession(session);
        session.setStatus(CheckoutSessionStatus.CANCELLED);
        checkoutSessionRepository.save(session);
    }

    @Override
    @Transactional
    public OrderResponse devSimulateVnpayPaymentSuccess(String publicId) {
        if (!vnpayProperties.isDevSimulateSuccessEnabled()) {
            throw new CustomApiException(
                    HttpStatus.FORBIDDEN,
                    "Dev simulate is disabled. Set vnpay.dev-simulate-success-enabled=true (local only; never in production).");
        }
        String pub = normalizeCheckoutSessionPublicId(publicId);
        UserEntity currentUser = userService.getCurrentUser();
        long userId = currentUser.getId();
        CheckoutSessionEntity session = checkoutSessionRepository
                .findByPublicIdAndUser_Id(pub, userId)
                .orElseThrow(() -> new NotFoundEntityException(
                        "Checkout session not found or not owned by current user"));

        if (session.getStatus() == CheckoutSessionStatus.COMPLETED && session.getOrderId() != null) {
            return buildOrderResponseForUser(session.getOrderId(), userId);
        }

        VnpaySessionFinalizeResult r = finalizeVnpayCheckoutSession(session.getId(), true);
        if (r == VnpaySessionFinalizeResult.CREATED || r == VnpaySessionFinalizeResult.ALREADY_COMPLETED) {
            CheckoutSessionEntity updated = checkoutSessionRepository
                    .findByPublicIdAndUser_Id(pub, userId)
                    .orElseThrow(() -> new NotFoundEntityException("Checkout session not found after dev simulate"));
            if (updated.getOrderId() == null) {
                throw new CustomApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Order was not linked after dev simulate");
            }
            return buildOrderResponseForUser(updated.getOrderId(), userId);
        }
        if (r == VnpaySessionFinalizeResult.NOT_FOUND) {
            throw new NotFoundEntityException("Checkout session not found");
        }
        if (r == VnpaySessionFinalizeResult.NOT_PAYABLE) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Session is not payable (not PENDING, or status FAILED/EXPIRED/CANCELLED, or not VNPAY).");
        }
        if (r == VnpaySessionFinalizeResult.BUSINESS_ERROR) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST, "Could not create order from session payload (validation or data error).");
        }
        throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected finalize result: " + r);
    }

    private OrderResponse buildOrderResponseForUser(long orderId, long userId) {
        OrderEntity order = orderRepository
                .findByIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new NotFoundEntityException("Order not found"));
        List<OrderDetailEntity> details = orderDetailRepository.findByOrder_IdOrderByIdAsc(order.getId());
        return buildOrderResponse(order, mapDetailResponses(details));
    }

    /**
     * Gắn trừ tồn giữ / lock theo {@code checkout_session} tại đây (ví dụ theo {@link CheckoutSessionEntity#getId()}
     * hoặc {@link CheckoutSessionEntity#getPublicId()}) rồi trả tồn khi hủy.
     */
    private void releaseWarehouseLockForAbandonedCheckoutSession(CheckoutSessionEntity session) {
        Objects.requireNonNull(session, "session");
        // Khi bảng tồn/warehouse nối với session: implement release tại đây; gọi trước khi gán CANCELLED + save.
    }

    private VnpayPendingTransactionResponse toPendingFromSession(CheckoutSessionEntity session, String publicId) {
        PaymentMethodEntity pm = session.getPaymentMethod();
        PaymentMethodSummaryResponse pmSum = pm == null
                ? null
                : PaymentMethodSummaryResponse.builder()
                        .id(pm.getId())
                        .name(pm.getName())
                        .code(pm.getCode())
                        .build();
        String vnp = String.valueOf(session.getId());

        if (session.getStatus() == CheckoutSessionStatus.COMPLETED && session.getOrderId() != null) {
            OrderEntity o = orderRepository
                    .findByIdAndUser_Id(session.getOrderId(), session.getUser().getId())
                    .orElse(null);
            if (o != null) {
                return toPendingCompleted(publicId, o);
            }
        }
        if (session.getStatus() == CheckoutSessionStatus.FAILED) {
            return VnpayPendingTransactionResponse.builder()
                    .transactionPublicId(publicId)
                    .state("FAILED")
                    .expiresAt(session.getExpiresAt())
                    .total(session.getTotal())
                    .paymentMethod(pmSum)
                    .vnpayTxnRef(vnp)
                    .orderInfo(buildOrderInfoFromSessionPayload(session))
                    .message("Payment was not successful at VNPAY.")
                    .build();
        }
        if (session.getStatus() == CheckoutSessionStatus.EXPIRED) {
            return VnpayPendingTransactionResponse.builder()
                    .transactionPublicId(publicId)
                    .state("EXPIRED")
                    .expiresAt(session.getExpiresAt())
                    .total(session.getTotal())
                    .paymentMethod(pmSum)
                    .vnpayTxnRef(vnp)
                    .orderInfo(buildOrderInfoFromSessionPayload(session))
                    .message("This checkout session has expired.")
                    .build();
        }
        if (session.getStatus() == CheckoutSessionStatus.CANCELLED) {
            return VnpayPendingTransactionResponse.builder()
                    .transactionPublicId(publicId)
                    .state("CANCELLED")
                    .expiresAt(session.getExpiresAt())
                    .total(session.getTotal())
                    .paymentMethod(pmSum)
                    .vnpayTxnRef(vnp)
                    .orderInfo(buildOrderInfoFromSessionPayload(session))
                    .message("This checkout session was cancelled.")
                    .build();
        }
        if (session.getStatus() == CheckoutSessionStatus.PENDING) {
            Date now = new Date();
            if (session.getExpiresAt() != null && session.getExpiresAt().before(now)) {
                return VnpayPendingTransactionResponse.builder()
                        .transactionPublicId(publicId)
                        .state("EXPIRED")
                        .expiresAt(session.getExpiresAt())
                        .total(session.getTotal())
                        .paymentMethod(pmSum)
                        .vnpayTxnRef(vnp)
                        .orderInfo(buildOrderInfoFromSessionPayload(session))
                        .message("This checkout session has expired.")
                        .build();
            }
            return VnpayPendingTransactionResponse.builder()
                    .transactionPublicId(publicId)
                    .state("PENDING")
                    .expiresAt(session.getExpiresAt())
                    .total(session.getTotal())
                    .paymentMethod(pmSum)
                    .vnpayTxnRef(vnp)
                    .orderInfo(buildOrderInfoFromSessionPayload(session))
                    .message("Waiting for VNPAY payment (order is created when payment succeeds).")
                    .build();
        }
        return VnpayPendingTransactionResponse.builder()
                .transactionPublicId(publicId)
                .state(session.getStatus().name())
                .expiresAt(session.getExpiresAt())
                .total(session.getTotal())
                .paymentMethod(pmSum)
                .vnpayTxnRef(vnp)
                .orderInfo(buildOrderInfoFromSessionPayload(session))
                .build();
    }

    private VnpayPendingTransactionResponse toPendingCompleted(String publicId, OrderEntity order) {
        List<OrderDetailEntity> details = orderDetailRepository.findByOrder_IdOrderByIdAsc(order.getId());
        List<OrderDetailResponse> detailResponses = mapDetailResponses(details);
        OrderResponse orderRes = buildOrderResponse(order, detailResponses);
        return VnpayPendingTransactionResponse.builder()
                .transactionPublicId(publicId)
                .state("COMPLETED")
                .orderInfo(toCheckoutOrderInfoFromEntity(order, detailResponses))
                .order(orderRes)
                .message("Payment successful; order is available in your order list.")
                .build();
    }

    /**
     * Điền dòng hàng, địa chỉ, tổng từ {@link CheckoutSessionEntity#getRequestPayloadJson()} cùng giá tại
     * thời điểm tra cứu. Trả null nếu parse lỗi hoặc sản phẩm thay đổi/đã bị gỡ.
     */
    private VnpayCheckoutOrderInfoResponse buildOrderInfoFromSessionPayload(CheckoutSessionEntity session) {
        if (session == null || session.getRequestPayloadJson() == null) {
            return null;
        }
        try {
            CreateOrderRequest req = objectMapper.readValue(
                    session.getRequestPayloadJson(), CreateOrderRequest.class);
            OrderPlaceDraft draft = buildDraft(req);
            CreateOrderHeaderRequest h = draft.header();
            int typeOrder = h.getTypeOrder() != null ? h.getTypeOrder() : OrderConstants.TYPE_ONLINE;
            CheckoutShipping ship = resolveCheckoutShipping(h, session.getUser().getId(), session);
            return VnpayCheckoutOrderInfoResponse.builder()
                    .description(h.getDescription())
                    .typeOrder(typeOrder)
                    .deliveryAddress(ship.deliveryAddressForOrder())
                    .total(session.getTotal())
                    .deliveryDistanceMeters(session.getDeliveryDistanceMeters())
                    .shippingFeeVnd(session.getShippingFeeVnd())
                    .orderDetails(draft.detailResponses())
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private static VnpayCheckoutOrderInfoResponse toCheckoutOrderInfoFromEntity(
            OrderEntity order, List<OrderDetailResponse> orderDetails) {
        return VnpayCheckoutOrderInfoResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .description(order.getDescription())
                .typeOrder(order.getTypeOrder())
                .deliveryAddress(order.getDeliveryAddress())
                .total(order.getTotal())
                .deliveryDistanceMeters(order.getDeliveryDistanceMeters())
                .shippingFeeVnd(order.getShippingFeeVnd())
                .orderDetails(orderDetails)
                .build();
    }

    private OrderPlaceDraft buildDraft(CreateOrderRequest request) {
        List<Long> productIds = request.getOrderDetails().stream()
                .map(CreateOrderDetailRequest::getProductId)
                .distinct()
                .collect(Collectors.toList());

        List<ProductEntity> products = productRepository.findAllWithFullRelationsByIdIn(productIds);
        Map<Long, ProductEntity> byId = products.stream()
                .collect(Collectors.toMap(ProductEntity::getId, p -> p));

        double orderTotal = 0.0;
        List<OrderDetailResponse> detailResponses = new ArrayList<>();
        List<OrderDetailEntity> detailsToSave = new ArrayList<>();

        for (CreateOrderDetailRequest line : request.getOrderDetails()) {
            ProductEntity product = byId.get(line.getProductId());
            if (product == null) {
                throw new NotFoundEntityException("Product not found with id: " + line.getProductId());
            }
            if (product.getStatus() == null || !product.getStatus().equals(SystemConstant.ACTIVE_STATUS)) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Product is not available: " + line.getProductId());
            }
            double unitPrice = resolveUnitPrice(product);
            double lineTotalValue = unitPrice * line.getQuantity();
            orderTotal += lineTotalValue;
            String lineTotalStr = String.valueOf(lineTotalValue);

            OrderDetailEntity detail = OrderDetailEntity.builder()
                    .order(null)
                    .product(product)
                    .quantity(line.getQuantity())
                    .unitPrice(unitPrice)
                    .description(line.getDescription())
                    .totalPrice(lineTotalStr)
                    .build();
            detailsToSave.add(detail);

            detailResponses.add(OrderDetailResponse.builder()
                    .productId(product.getId())
                    .productName(product.getProductName())
                    .quantity(line.getQuantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotalStr)
                    .description(line.getDescription())
                    .build());
        }

        CreateOrderHeaderRequest header = request.getOrder();
        PaymentMethodEntity paymentMethod = paymentMethodRepository
                .findById(header.getPaymentMethodId())
                .orElseThrow(() -> new NotFoundEntityException(
                        "Payment method not found with id: " + header.getPaymentMethodId()));
        if (!Boolean.TRUE.equals(paymentMethod.getActive())) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Payment method is not available");
        }

        return new OrderPlaceDraft(orderTotal, detailsToSave, detailResponses, header, paymentMethod);
    }

    private OrderEntity persistOrderFromDraft(
            OrderPlaceDraft draft,
            UserEntity currentUser,
            boolean markPaid,
            Date paidAt) {
        return persistOrderFromDraft(draft, currentUser, markPaid, paidAt, null, null, null);
    }

    private OrderEntity persistOrderFromDraft(
            OrderPlaceDraft draft,
            UserEntity currentUser,
            boolean markPaid,
            Date paidAt,
            String checkoutSessionPublicId,
            Long vnpayCheckoutTxnRef,
            CheckoutSessionEntity vnpaySessionOrNull) {
        CreateOrderHeaderRequest header = draft.header();
        String orderDescription = header.getDescription();
        int typeOrder = header.getTypeOrder() != null
                ? header.getTypeOrder()
                : OrderConstants.TYPE_ONLINE;

        CheckoutShipping ship = resolveCheckoutShipping(header, currentUser.getId(), vnpaySessionOrNull);
        double grandTotal = grandOrderTotalVnd(draft.orderTotal(), ship.shippingFeeVnd());

        OrderEntity order = OrderEntity.builder()
                .user(currentUser)
                .status(OrderConstants.STATUS_AWAITING_CONFIRM)
                .description(orderDescription)
                .total(grandTotal)
                .typeOrder(typeOrder)
                .deliveryAddress(ship.deliveryAddressForOrder())
                .deliveryDistanceMeters(ship.distanceMeters())
                .shippingFeeVnd(ship.shippingFeeVnd())
                .paymentMethod(draft.paymentMethod())
                .paid(markPaid)
                .paidAt(markPaid ? paidAt : null)
                .checkoutSessionPublicId(checkoutSessionPublicId)
                .vnpayCheckoutTxnRef(vnpayCheckoutTxnRef)
                .build();

        order = orderRepository.save(order);
        order.setOrderCode(String.format("DH-%d-%08d", Year.now().getValue(), order.getId()));
        order = orderRepository.save(order);

        List<OrderDetailEntity> detailsToSave = draft.detailsToSave();
        for (OrderDetailEntity d : detailsToSave) {
            d.setOrder(order);
        }
        orderDetailRepository.saveAll(detailsToSave);
        return order;
    }

    private record OrderPlaceDraft(
            double orderTotal,
            List<OrderDetailEntity> detailsToSave,
            List<OrderDetailResponse> detailResponses,
            CreateOrderHeaderRequest header,
            PaymentMethodEntity paymentMethod) {
    }

    /**
     * Khoảng cách + phí + chuỗi địa chỉ lưu trên đơn (địa chỉ lấy từ {@code user_address} khi có id).
     */
    private record CheckoutShipping(Double distanceMeters, Long shippingFeeVnd, String deliveryAddressForOrder) {}

    /** Tổng thanh toán (VND): tiền hàng (chi tiết) + phí ship; phí null coi như 0. */
    private static double grandOrderTotalVnd(double merchandiseSubtotal, Long shippingFeeVnd) {
        long shipVnd = shippingFeeVnd != null ? shippingFeeVnd : 0L;
        return merchandiseSubtotal + shipVnd;
    }

    private void validateOrderHeaderForShipping(CreateOrderHeaderRequest h) {
        if (h.getUserAddressId() != null) {
            return;
        }
        if (h.getDeliveryDistanceMeters() != null) {
            if (h.getDeliveryAddress() == null || h.getDeliveryAddress().isBlank()) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "deliveryAddress is required when using deliveryDistanceMeters without userAddressId");
            }
            return;
        }
        throw new CustomApiException(HttpStatus.BAD_REQUEST,
                "Provide userAddressId (saved address) or deliveryDistanceMeters from GET .../shipping/distance-to-warehouse");
    }

    private static String normalizeOptionalCheckoutWorkSessionId(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > 128) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "checkoutWorkSessionId must be at most 128 characters");
        }
        if (!WORK_SESSION_PUBLIC_ID_PATTERN.matcher(t).matches()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "checkoutWorkSessionId may only contain letters, digits, hyphen, underscore, dot, colon");
        }
        return t;
    }

    private static final java.util.regex.Pattern WORK_SESSION_PUBLIC_ID_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9._:\\-]+$");

    /** {@code public_id} = id phiên (chuỗi); từ chối rỗng / quá dài. */
    private static String normalizeCheckoutSessionPublicId(String publicId) {
        if (publicId == null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Invalid transaction public id");
        }
        String t = publicId.trim();
        if (t.isEmpty() || t.length() > 128) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Invalid transaction public id");
        }
        return t;
    }

    /**
     * Ưu tiên snapshot phiên VNPAY (nếu có khoảng cách hoặc phí); không gọi OSM/OSRM.
     * Ngược lại: ưu tiên {@code userAddressId} (đọc {@code distance_to_warehouse_meters}, {@code shipping_fee_vnd} trên DB).
     */
    private CheckoutShipping resolveCheckoutShipping(
            CreateOrderHeaderRequest header,
            long userId,
            CheckoutSessionEntity vnpaySessionOrNull) {
        if (vnpaySessionOrNull != null
                && (vnpaySessionOrNull.getDeliveryDistanceMeters() != null
                        || vnpaySessionOrNull.getShippingFeeVnd() != null)) {
            return new CheckoutShipping(
                    vnpaySessionOrNull.getDeliveryDistanceMeters(),
                    vnpaySessionOrNull.getShippingFeeVnd(),
                    resolveDeliveryAddressForOrder(header, userId));
        }
        return resolveCheckoutShippingFromHeader(header, userId);
    }

    private CheckoutShipping resolveCheckoutShippingFromHeader(CreateOrderHeaderRequest header, long userId) {
        if (header.getUserAddressId() != null) {
            UserAddressEntity addr = userAddressRepository
                    .findByIdAndUserIdAndAddressType(header.getUserAddressId(), userId, AddressType.USER)
                    .orElseThrow(() -> new NotFoundEntityException(
                            "User address not found with id: " + header.getUserAddressId()));
            Double dist = addr.getDistanceToWarehouseMeters();
            Long fee = addr.getShippingFeeVnd();
            if (dist == null && header.getDeliveryDistanceMeters() != null) {
                dist = header.getDeliveryDistanceMeters();
                fee = ShippingFeeCalculator.fromDistanceMeters(dist);
            } else if (dist != null) {
                if (fee == null) {
                    fee = ShippingFeeCalculator.fromDistanceMeters(dist);
                }
            } else {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Saved address has no distance to warehouse; update the address or send deliveryDistanceMeters");
            }
            return new CheckoutShipping(dist, fee, formatUserAddressSnapshot(addr));
        }
        if (header.getDeliveryDistanceMeters() != null) {
            double m = header.getDeliveryDistanceMeters();
            if (header.getDeliveryAddress() == null || header.getDeliveryAddress().isBlank()) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "deliveryAddress is required when using deliveryDistanceMeters without userAddressId");
            }
            return new CheckoutShipping(
                    m, ShippingFeeCalculator.fromDistanceMeters(m), header.getDeliveryAddress().trim());
        }
        throw new CustomApiException(HttpStatus.BAD_REQUEST,
                "Provide userAddressId (saved address) or deliveryDistanceMeters from the shipping API");
    }

    private String resolveDeliveryAddressForOrder(CreateOrderHeaderRequest header, long userId) {
        if (header.getUserAddressId() != null) {
            UserAddressEntity addr = userAddressRepository
                    .findByIdAndUserIdAndAddressType(header.getUserAddressId(), userId, AddressType.USER)
                    .orElseThrow(() -> new NotFoundEntityException(
                            "User address not found with id: " + header.getUserAddressId()));
            return formatUserAddressSnapshot(addr);
        }
        if (header.getDeliveryAddress() == null || header.getDeliveryAddress().isBlank()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "deliveryAddress is required when userAddressId is omitted");
        }
        return header.getDeliveryAddress().trim();
    }

    private static String formatUserAddressSnapshot(UserAddressEntity a) {
        return Stream.of(a.getAddressLine(), a.getCity(), a.getState(), a.getCountry(), a.getZipCode())
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(", "));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Integer status) {
        if (status != null && (status < OrderConstants.STATUS_AWAITING_CONFIRM
                || status > OrderConstants.STATUS_CANCELLED)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "status must be between 1 and 5");
        }
        UserEntity currentUser = userService.getCurrentUser();
        List<OrderEntity> orders = status == null
                ? orderRepository.findByUser_IdOrderByIdDesc(currentUser.getId())
                : orderRepository.findByUser_IdAndStatusOrderByIdDesc(currentUser.getId(), status);
        return orders.stream()
                .map(o -> buildOrderResponse(o, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(Long id) {
        UserEntity currentUser = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUser_Id(id, currentUser.getId())
                .orElseThrow(() -> new NotFoundEntityException("Order not found with id: " + id));
        List<OrderDetailEntity> details = orderDetailRepository.findByOrder_IdOrderByIdAsc(order.getId());
        return buildOrderResponse(order, mapDetailResponses(details));
    }

    @Override
    @Transactional
    public OrderResponse requestReturn(Long orderId, OrderReturnRequest request) {
        UserEntity currentUser = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUser_Id(orderId, currentUser.getId())
                .orElseThrow(() -> new NotFoundEntityException("Order not found with id: " + orderId));

        if (order.getStatus() != null
                && order.getStatus() == OrderConstants.STATUS_CANCELLED) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Order is cancelled");
        }
        if (order.getReturnRefundStatus() != null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Return or refund request already exists for this order");
        }
        if (!Boolean.TRUE.equals(order.getPaid()) || order.getPaidAt() == null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Return / refund is only available after payment is recorded");
        }
        if (!isWithinReturnWindowAfterPaid(order.getPaidAt())) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Return / refund is only available within 7 days after payment");
        }

        order.setReturnRefundStatus(OrderConstants.RETURN_STATUS_REQUESTED);
        order.setReturnRefundNote(request.getReason());
        order = orderRepository.save(order);

        List<OrderDetailEntity> details = orderDetailRepository.findByOrder_IdOrderByIdAsc(order.getId());
        return buildOrderResponse(order, mapDetailResponses(details));
    }

    @Override
    @Transactional
    public OrderResponse cancelMyOrder(Long orderId) {
        UserEntity currentUser = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUser_Id(orderId, currentUser.getId())
                .orElseThrow(() -> new NotFoundEntityException("Order not found with id: " + orderId));
        if (order.getStatus() != null
                && order.getStatus() == OrderConstants.STATUS_CANCELLED) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Order is already cancelled");
        }
        if (order.getStatus() == null
                || order.getStatus() != OrderConstants.STATUS_AWAITING_CONFIRM) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Cancellations are only allowed while status is awaiting confirmation (1)");
        }
        if (Boolean.TRUE.equals(order.getPaid())) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel: payment has already been recorded for this order");
        }
        order.setStatus(OrderConstants.STATUS_CANCELLED);
        order = orderRepository.save(order);
        List<OrderDetailEntity> details = orderDetailRepository.findByOrder_IdOrderByIdAsc(order.getId());
        return buildOrderResponse(order, mapDetailResponses(details));
    }

    @Override
    @Transactional
    public OrderResponse confirmPayment(Long orderId) {
        UserEntity currentUser = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUser_Id(orderId, currentUser.getId())
                .orElseThrow(() -> new NotFoundEntityException("Order not found with id: " + orderId));
        if (order.getStatus() != null
                && order.getStatus() == OrderConstants.STATUS_CANCELLED) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Order is cancelled");
        }
        if (Boolean.TRUE.equals(order.getPaid())) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Payment is already recorded");
        }
        order.setPaid(true);
        order.setPaidAt(new Date());
        order = orderRepository.save(order);
        List<OrderDetailEntity> details = orderDetailRepository.findByOrder_IdOrderByIdAsc(order.getId());
        return buildOrderResponse(order, mapDetailResponses(details));
    }

    private static boolean isWithinReturnWindowAfterPaid(Date paidAt) {
        if (paidAt == null) {
            return false;
        }
        Instant p = paidAt.toInstant();
        Instant end = p.plus(OrderConstants.RETURN_ELIGIBLE_DAYS_AFTER_PAID, ChronoUnit.DAYS);
        return !Instant.now().isAfter(end);
    }

    private OrderResponse buildOrderResponse(OrderEntity order, List<OrderDetailResponse> orderDetails) {
        PaymentMethodEntity pm = order.getPaymentMethod();
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .returnRefundStatus(order.getReturnRefundStatus())
                .returnRefundNote(order.getReturnRefundNote())
                .description(order.getDescription())
                .total(order.getTotal())
                .typeOrder(order.getTypeOrder())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryDistanceMeters(order.getDeliveryDistanceMeters())
                .shippingFeeVnd(order.getShippingFeeVnd())
                .paid(order.getPaid())
                .paidAt(order.getPaidAt())
                .paymentMethod(pm == null ? null : PaymentMethodSummaryResponse.builder()
                        .id(pm.getId())
                        .name(pm.getName())
                        .code(pm.getCode())
                        .build())
                .orderDetails(orderDetails)
                .createdDate(order.getCreatedDate())
                .modifiedDate(order.getModifiedDate())
                .build();
    }

    private static List<OrderDetailResponse> mapDetailResponses(List<OrderDetailEntity> details) {
        return details.stream()
                .map(d -> OrderDetailResponse.builder()
                        .id(d.getId())
                        .productId(d.getProduct().getId())
                        .productName(d.getProduct().getProductName())
                        .quantity(d.getQuantity())
                        .unitPrice(d.getUnitPrice())
                        .lineTotal(d.getTotalPrice())
                        .description(d.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    private static double resolveUnitPrice(ProductEntity product) {
        if (product.getPrices() == null || product.getPrices().isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Product has no price: " + product.getId());
        }
        return product.getPrices().stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparing(PriceEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(PriceEntity::getCurrentValue)
                .orElseThrow(() -> new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Product has no price: " + product.getId()));
    }
}
