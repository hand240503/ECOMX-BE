package com.ndh.ShopTechnology.services.order.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndh.ShopTechnology.config.VnpayProperties;
import com.ndh.ShopTechnology.constants.OrderConstants;
import com.ndh.ShopTechnology.constants.SystemConstant;
import com.ndh.ShopTechnology.dto.request.order.CreateOrderDetailRequest;
import com.ndh.ShopTechnology.dto.request.order.CreateOrderHeaderRequest;
import com.ndh.ShopTechnology.dto.request.order.CheckoutPricingPreviewRequest;
import com.ndh.ShopTechnology.dto.request.order.CreateOrderRequest;
import com.ndh.ShopTechnology.dto.request.order.OrderReturnRequest;
import com.ndh.ShopTechnology.dto.response.order.CheckoutPricingLineItemResponse;
import com.ndh.ShopTechnology.dto.response.order.CheckoutPricingPreviewResponse;
import com.ndh.ShopTechnology.dto.response.order.CreateOrderResultResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderDetailResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderTimelineResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderTimelineResponse.TimelineStep;
import com.ndh.ShopTechnology.dto.response.order.PaymentMethodSummaryResponse;
import com.ndh.ShopTechnology.entities.log.OrderHistoryEntity;
import com.ndh.ShopTechnology.dto.response.order.VnpayCheckoutOrderInfoResponse;
import com.ndh.ShopTechnology.dto.response.order.VnpayPendingTransactionResponse;
import com.ndh.ShopTechnology.dto.response.order.OrderLinePricingProgramsDto;
import com.ndh.ShopTechnology.dto.response.order.VnpayTransactionStatusResponse;
import com.ndh.ShopTechnology.entities.order.CheckoutSessionEntity;
import com.ndh.ShopTechnology.entities.order.CheckoutSessionStatus;
import com.ndh.ShopTechnology.entities.order.OrderDetailEntity;
import com.ndh.ShopTechnology.entities.order.OrderEntity;
import com.ndh.ShopTechnology.entities.order.PaymentMethodEntity;
import com.ndh.ShopTechnology.entities.product.ProductEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
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
import com.ndh.ShopTechnology.repository.ProductVariantRepository;
import com.ndh.ShopTechnology.repository.UserAddressRepository;
import com.ndh.ShopTechnology.utils.ShippingFeeCalculator;
import com.ndh.ShopTechnology.services.log.OrderHistoryService;
import com.ndh.ShopTechnology.services.order.OrderService;
import com.ndh.ShopTechnology.services.order.VnpaySessionFinalizeResult;
import com.ndh.ShopTechnology.services.promotion.PromotionPricingService;
import com.ndh.ShopTechnology.services.task.OrderCancelledEvent;
import com.ndh.ShopTechnology.services.task.OrderCreatedEvent;
import com.ndh.ShopTechnology.services.task.OrderReturnedEvent;
import com.ndh.ShopTechnology.services.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ndh.ShopTechnology.entities.product.ProductPriceChangeUsageEntity;
import com.ndh.ShopTechnology.repository.ProductPriceChangeUsageRepository;
import com.ndh.ShopTechnology.services.product.ProductPriceChangeService;
import com.ndh.ShopTechnology.services.product.impl.ProductImageAttachService;
import com.ndh.ShopTechnology.services.promotion.PromotionPricingService.PricingContext;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final int CHECKOUT_SESSION_VALID_MINUTES = 30;

    private static final String VNP_TXN_ST_SUCCESS = "00";
    private static final String VNP_TXN_ST_NOT_FINISHED = "01";
    private static final String VNP_TXN_ST_ERROR = "02";
    private static final String VNP_TXN_MSG_SUCCESS = "Giao dịch thành công";
    private static final String VNP_TXN_MSG_NOT_FINISHED = "Giao dịch chưa hoàn tất";
    private static final String VNP_TXN_MSG_ERROR = "Giao dịch bị lỗi";

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final VnpayProperties vnpayProperties;
    private final UserAddressRepository userAddressRepository;
    private final PromotionPricingService promotionPricingService;
    private final ProductPriceChangeService priceChangeService;
    private final ProductPriceChangeUsageRepository priceChangeUsageRepository;
    private final OrderHistoryService orderHistoryService;
    private final com.ndh.ShopTechnology.repository.OrderHistoryRepository orderHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProductImageAttachService productImageAttachService;
    private final com.ndh.ShopTechnology.services.notification.NotificationService notificationService;
    private final com.ndh.ShopTechnology.services.inventory.InventoryService inventoryService;
    private final com.ndh.ShopTechnology.repository.DocumentRepository documentRepository;
    private final com.ndh.ShopTechnology.services.storage.CloudinaryService cloudinaryService;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderDetailRepository orderDetailRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            PaymentMethodRepository paymentMethodRepository,
            CheckoutSessionRepository checkoutSessionRepository,
            UserService userService,
            ObjectMapper objectMapper,
            VnpayProperties vnpayProperties,
            UserAddressRepository userAddressRepository,
            PromotionPricingService promotionPricingService,
            ProductPriceChangeService priceChangeService,
            ProductPriceChangeUsageRepository priceChangeUsageRepository,
            OrderHistoryService orderHistoryService,
            com.ndh.ShopTechnology.repository.OrderHistoryRepository orderHistoryRepository,
            ApplicationEventPublisher eventPublisher,
            ProductImageAttachService productImageAttachService,
            com.ndh.ShopTechnology.services.notification.NotificationService notificationService,
            com.ndh.ShopTechnology.services.inventory.InventoryService inventoryService,
            com.ndh.ShopTechnology.repository.DocumentRepository documentRepository,
            com.ndh.ShopTechnology.services.storage.CloudinaryService cloudinaryService) {
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.vnpayProperties = vnpayProperties;
        this.userAddressRepository = userAddressRepository;
        this.promotionPricingService = promotionPricingService;
        this.priceChangeService = priceChangeService;
        this.priceChangeUsageRepository = priceChangeUsageRepository;
        this.orderHistoryService = orderHistoryService;
        this.orderHistoryRepository = orderHistoryRepository;
        this.eventPublisher = eventPublisher;
        this.productImageAttachService = productImageAttachService;
        this.notificationService = notificationService;
        this.inventoryService = inventoryService;
        this.documentRepository = documentRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutPricingPreviewResponse previewCheckoutPricing(CheckoutPricingPreviewRequest request) {
        List<PromotionPricingService.OrderVariantLine> orderLines = resolveOrderLinesFromDetails(request.getLines());
        PromotionPricingService.PricingWithSuggestionsResult result =
                promotionPricingService.priceLinesWithProgramsAndSuggestions(orderLines);
        List<PromotionPricingService.PricedLineWithPrograms> priced = result.pricedLines();
        double subtotal = 0.0;
        List<CheckoutPricingLineItemResponse> out = new ArrayList<>();
        for (int i = 0; i < priced.size(); i++) {
            PromotionPricingService.PricedLineWithPrograms p = priced.get(i);
            PromotionPricingService.OrderVariantLine ol = orderLines.get(i);
            ProductVariantEntity v = ol.variant();
            ProductEntity prod = v.getProduct();
            subtotal += p.lineTotal();
            out.add(CheckoutPricingLineItemResponse.builder()
                    .productId(prod.getId())
                    .productVariantId(v.getId())
                    .productName(prod.getProductName())
                    .variantSkuCode(v.getSkuCode())
                    .variantOptions(v.getOptionValues())
                    .quantity(p.line().getQuantity())
                    .description(p.line().getDescription())
                    .unitPrice(p.finalUnitPrice())
                    .lineTotal(p.lineTotal())
                    .pricingPrograms(p.programs())
                    .build());
        }
        return CheckoutPricingPreviewResponse.builder()
                .lines(out)
                .itemsSubtotal(subtotal)
                .pwpSuggestions(result.pwpSuggestions())
                .build();
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

        validatePcPerCustomerFromDraft(draft, currentUser.getId());

        String pmCode = draft.paymentMethod().getCode() != null
                ? draft.paymentMethod().getCode().trim()
                : "";

        if (OrderConstants.PM_CODE_COD.equalsIgnoreCase(pmCode)) {
            OrderEntity order = persistOrderFromDraft(draft, currentUser, false, null);
            List<OrderDetailEntity> savedDetails = orderDetailRepository.findByOrder_IdOrderByIdAsc(order.getId());
            int totalQty   = savedDetails.stream().mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0).sum();
            eventPublisher.publishEvent(new OrderCreatedEvent(this, order.getId(),
                    order.getOrderCode(), savedDetails.size(), totalQty));
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
            // Kiểm tra tồn trước khi cho khách sang VNPAY (best-effort; chưa giữ chỗ, sẽ giữ khi thanh toán OK).
            validateDraftAvailability(draft.detailsToSave());
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
            List<OrderDetailEntity> vnDetails = orderDetailRepository.findByOrder_IdOrderByIdAsc(order.getId());
            int vnTotalQty = vnDetails.stream().mapToInt(d -> d.getQuantity() != null ? d.getQuantity() : 0).sum();
            eventPublisher.publishEvent(new OrderCreatedEvent(this, order.getId(),
                    order.getOrderCode(), vnDetails.size(), vnTotalQty));
            // Thông báo thanh toán VNPAY thành công.
            notificationService.notifyUser(
                    user,
                    "PAYMENT",
                    "Thanh toán thành công",
                    "Đơn hàng " + order.getOrderCode() + " đã được thanh toán qua VNPAY thành công.",
                    order.getId());
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

    private void releaseWarehouseLockForAbandonedCheckoutSession(CheckoutSessionEntity session) {
        Objects.requireNonNull(session, "session");
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

    private List<PromotionPricingService.OrderVariantLine> resolveOrderLines(CreateOrderRequest request) {
        return resolveOrderLinesFromDetails(request.getOrderDetails());
    }

    private List<PromotionPricingService.OrderVariantLine> resolveOrderLinesFromDetails(
            List<CreateOrderDetailRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "order lines must not be empty");
        }
        for (CreateOrderDetailRequest line : lines) {
            if (line.getProductVariantId() == null && line.getProductId() == null) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST,
                        "Each order line requires productVariantId or productId");
            }
        }
        List<Long> explicitVariantIds = lines.stream()
                .map(CreateOrderDetailRequest::getProductVariantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> legacyProductIds = lines.stream()
                .filter(l -> l.getProductVariantId() == null)
                .map(CreateOrderDetailRequest::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, ProductVariantEntity> byVid = productVariantRepository.findAllWithProductAndPricesByIdIn(
                explicitVariantIds).stream()
                .collect(Collectors.toMap(ProductVariantEntity::getId, v -> v));

        Map<Long, ProductVariantEntity> defaultByProduct = resolveDefaultVariantsForProducts(legacyProductIds);

        List<PromotionPricingService.OrderVariantLine> out = new ArrayList<>();
        for (CreateOrderDetailRequest line : lines) {
            ProductVariantEntity variant;
            if (line.getProductVariantId() != null) {
                variant = byVid.get(line.getProductVariantId());
                if (variant == null) {
                    throw new NotFoundEntityException("Product variant not found: " + line.getProductVariantId());
                }
            } else {
                variant = defaultByProduct.get(line.getProductId());
                if (variant == null) {
                    throw new NotFoundEntityException("No active variant for product: " + line.getProductId());
                }
            }
            ProductEntity product = variant.getProduct();
            if (product.getStatus() == null || !product.getStatus().equals(SystemConstant.ACTIVE_STATUS)) {
                throw new CustomApiException(HttpStatus.BAD_REQUEST, "Product is not available: " + product.getId());
            }
            out.add(new PromotionPricingService.OrderVariantLine(line, variant));
        }
        return out;
    }

    private Map<Long, ProductVariantEntity> resolveDefaultVariantsForProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductVariantEntity> list = productVariantRepository.findActiveByProductIdIn(productIds);
        Map<Long, ProductVariantEntity> first = new LinkedHashMap<>();
        for (ProductVariantEntity v : list) {
            first.putIfAbsent(v.getProduct().getId(), v);
        }
        for (Long pid : productIds) {
            if (!first.containsKey(pid)) {
                throw new NotFoundEntityException("No active variant for product: " + pid);
            }
        }
        return first;
    }

    private void validatePcPerCustomerFromDraft(OrderPlaceDraft draft, Long userId) {
        if (userId == null || draft.detailsToSave() == null) return;
        for (OrderDetailEntity d : draft.detailsToSave()) {
            if (d.getPricingProgramsJson() == null) continue;
            try {
                OrderLinePricingProgramsDto programs = objectMapper.readValue(
                        d.getPricingProgramsJson(), OrderLinePricingProgramsDto.class);
                if (programs == null || programs.getPriceChange() == null) continue;
                Long pcId = programs.getPriceChange().getId();
                if (pcId == null) continue;
                int qty = d.getQuantity() != null ? d.getQuantity() : 1;
                if (!priceChangeService.isWithinPerCustomerLimit(pcId, userId, qty)) {
                    throw new CustomApiException(HttpStatus.CONFLICT,
                            "Bạn đã đạt giới hạn số lượng cho phép theo giá ưu đãi đặc biệt này (đợt PC #"
                                    + pcId + "). Mỗi khách hàng chỉ được mua một số lượng nhất định.");
                }
            } catch (CustomApiException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("Could not validate PC per-customer limit for draft detail, skipping", ex);
            }
        }
    }

    private void validatePcPerCustomerLimits(
            List<PromotionPricingService.PricedLineWithPrograms> pricedLines,
            Long userId) {
        if (userId == null || pricedLines == null) return;
        for (PromotionPricingService.PricedLineWithPrograms pl : pricedLines) {
            if (pl.programs() == null || pl.programs().getPriceChange() == null) continue;
            Long pcId = pl.programs().getPriceChange().getId();
            if (pcId == null) continue;
            int qty = pl.line() != null && pl.line().getQuantity() != null ? pl.line().getQuantity() : 1;
            boolean within = priceChangeService.isWithinPerCustomerLimit(pcId, userId, qty);
            if (!within) {
                throw new CustomApiException(HttpStatus.CONFLICT,
                        "Bạn đã đạt giới hạn số lượng cho phép theo giá ưu đãi đặc biệt này (đợt PC #"
                                + pcId + "). Mỗi khách hàng chỉ được mua một số lượng nhất định.");
            }
        }
    }

    private OrderPlaceDraft buildDraft(CreateOrderRequest request) {
        CreateOrderHeaderRequest headerForPm = request.getOrder();
        PaymentMethodEntity paymentMethodEarly = paymentMethodRepository
                .findById(headerForPm.getPaymentMethodId())
                .orElseThrow(() -> new NotFoundEntityException(
                        "Payment method not found with id: " + headerForPm.getPaymentMethodId()));
        if (!Boolean.TRUE.equals(paymentMethodEarly.getActive())) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Payment method is not available");
        }
        PricingContext pricingCtx = new PricingContext(paymentMethodEarly.getCode());

        List<PromotionPricingService.OrderVariantLine> orderLines = resolveOrderLines(request);
        List<PromotionPricingService.PricedLineWithPrograms> priced =
                promotionPricingService.priceLinesWithPrograms(orderLines, pricingCtx);

        double orderTotal = 0.0;
        List<OrderDetailResponse> detailResponses = new ArrayList<>();
        List<OrderDetailEntity> detailsToSave = new ArrayList<>();

        for (int i = 0; i < priced.size(); i++) {
            PromotionPricingService.PricedLineWithPrograms pl = priced.get(i);
            PromotionPricingService.OrderVariantLine ol = orderLines.get(i);
            CreateOrderDetailRequest line = pl.line();
            ProductVariantEntity variant = ol.variant();
            ProductEntity product = variant.getProduct();
            double unitPrice = pl.finalUnitPrice();
            double lineTotalValue = pl.lineTotal();
            orderTotal += lineTotalValue;
            String lineTotalStr = String.valueOf(lineTotalValue);

            String programsJson;
            try {
                programsJson = objectMapper.writeValueAsString(pl.programs());
            } catch (JsonProcessingException e) {
                throw new CustomApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Could not serialize pricing programs");
            }

            OrderDetailEntity detail = OrderDetailEntity.builder()
                    .order(null)
                    .product(product)
                    .productVariant(variant)
                    .quantity(line.getQuantity())
                    .unitPrice(unitPrice)
                    .description(line.getDescription())
                    .totalPrice(lineTotalStr)
                    .pricingProgramsJson(programsJson)
                    .build();
            detailsToSave.add(detail);

            detailResponses.add(OrderDetailResponse.builder()
                    .productId(product.getId())
                    .productVariantId(variant.getId())
                    .variantSkuCode(variant.getSkuCode())
                    .variantOptions(variant.getOptionValues())
                    .productName(product.getProductName())
                    .quantity(line.getQuantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotalStr)
                    .description(line.getDescription())
                    .lDescription(product.getLDescription())
                    .pricingPrograms(pl.programs())
                    .build());
        }

        CreateOrderHeaderRequest header = request.getOrder();
        return new OrderPlaceDraft(orderTotal, detailsToSave, detailResponses, header, paymentMethodEarly);
    }

    private OrderEntity persistOrderFromDraft(
            OrderPlaceDraft draft,
            UserEntity currentUser,
            boolean markPaid,
            Date paidAt) {
        return persistOrderFromDraft(draft, currentUser, markPaid, paidAt, null, null, null);
    }

    /** Kiểm tra còn đủ hàng bán (available) cho các dòng trong draft. Ném CONFLICT nếu thiếu. */
    private void validateDraftAvailability(List<OrderDetailEntity> details) {
        if (details == null) return;
        for (OrderDetailEntity d : details) {
            int qty = d.getQuantity() != null ? d.getQuantity() : 0;
            if (qty <= 0 || d.getProductVariant() == null || d.getProductVariant().getId() == null) continue;
            ProductVariantEntity v = productVariantRepository
                    .findById(d.getProductVariant().getId())
                    .orElse(null);
            if (v == null) continue;
            if (v.getAvailable() < qty) {
                String name = v.getProduct() != null && v.getProduct().getProductName() != null
                        ? v.getProduct().getProductName() : ("#" + v.getId());
                throw new CustomApiException(HttpStatus.CONFLICT,
                        "Không đủ hàng trong kho cho sản phẩm '" + name + "'. Cần " + qty
                                + ", còn lại " + v.getAvailable() + ".");
            }
        }
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

        final Long finalOrderId = order.getId();
        final Long currentUserId = currentUser.getId();
        for (OrderDetailEntity d : detailsToSave) {
            OrderLinePricingProgramsDto programs = null;
            try {
                if (d.getPricingProgramsJson() != null) {
                    programs = objectMapper.readValue(d.getPricingProgramsJson(), OrderLinePricingProgramsDto.class);
                }
            } catch (Exception ex) {
                log.warn("Could not deserialize pricingProgramsJson for order detail, skipping PC quota update", ex);
            }
            if (programs == null || programs.getPriceChange() == null) continue;
            Long pcId = programs.getPriceChange().getId();
            if (pcId == null) continue;
            int qty = d.getQuantity() != null ? d.getQuantity() : 1;
            boolean ok = priceChangeService.incrementSoldQuantity(pcId, qty);
            if (!ok) {
                log.warn("PC quota may be exceeded for priceChangeId={} on order={} — sold_quantity increment returned 0 rows affected",
                        pcId, finalOrderId);
            }
            priceChangeUsageRepository.save(ProductPriceChangeUsageEntity.builder()
                    .priceChangeId(pcId)
                    .userId(currentUserId)
                    .orderId(finalOrderId)
                    .quantity(qty)
                    .build());
        }

        // Đồng bộ kho: GIỮ hàng cho đơn vừa tạo.
        // - COD (chưa thanh toán): không đủ tồn → ném CONFLICT, rollback việc tạo đơn (chống bán âm).
        // - VNPAY (đã thanh toán): giữ chỗ best-effort, cho phép oversell, không từ chối đơn đã trả tiền.
        inventoryService.reserveForOrder(detailsToSave, !markPaid);

        return order;
    }

    private record OrderPlaceDraft(
            double orderTotal,
            List<OrderDetailEntity> detailsToSave,
            List<OrderDetailResponse> detailResponses,
            CreateOrderHeaderRequest header,
            PaymentMethodEntity paymentMethod) {
    }

    private record CheckoutShipping(Double distanceMeters, Long shippingFeeVnd, String deliveryAddressForOrder) {}

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
    @Transactional(readOnly = true)
    public OrderTimelineResponse getMyOrderTimeline(Long orderId) {
        UserEntity currentUser = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUser_Id(orderId, currentUser.getId())
                .orElseThrow(() -> new NotFoundEntityException("Order not found with id: " + orderId));

        int currentStatus = order.getStatus() != null ? order.getStatus() : OrderConstants.STATUS_AWAITING_CONFIRM;
        boolean cancelled = currentStatus == OrderConstants.STATUS_CANCELLED;

        // ── Lấy toàn bộ lịch sử thay đổi trạng thái đơn hàng ─────────────────
        // Map: newStatus → entry đầu tiên đạt status đó (lấy sớm nhất)
        List<OrderHistoryEntity> statusHistory = orderHistoryRepository
                .findByOrderIdAndChangeTypeOrderByCreatedAtDesc(
                        orderId, OrderHistoryEntity.CHANGE_TYPE_ORDER_STATUS);

        // Lấy bản ghi sớm nhất (index cuối vì sort DESC) cho từng newStatus
        Map<Integer, OrderHistoryEntity> firstEntryByStatus = new java.util.LinkedHashMap<>();
        for (int i = statusHistory.size() - 1; i >= 0; i--) {
            OrderHistoryEntity e = statusHistory.get(i);
            if (e.getNewStatus() != null) {
                firstEntryByStatus.putIfAbsent(e.getNewStatus(), e);
            }
        }

        // ── Lịch sử trả hàng / hoàn tiền (nếu có) ─────────────────────────────
        List<OrderHistoryEntity> returnHistory = orderHistoryRepository
                .findByOrderIdAndChangeTypeOrderByCreatedAtDesc(
                        orderId, OrderHistoryEntity.CHANGE_TYPE_RETURN_REFUND_STATUS);
        OrderHistoryEntity latestReturnEntry = returnHistory.isEmpty() ? null : returnHistory.get(0);
        boolean hasReturn = order.getReturnRefundStatus() != null
                && order.getReturnRefundStatus() != OrderConstants.RETURN_STATUS_NONE;

        // ── Định nghĩa các bước timeline ──────────────────────────────────────
        // Luồng bình thường: 1 → 2 → 3 → 4 → (đánh giá virtual) → (trả hàng nếu có)
        // Luồng hủy:         1 → ... → 5
        final int RETURN_STEP_CODE = -1;
        record StepDef(int statusCode, String label) {}

        List<StepDef> stepDefs = new ArrayList<>();
        stepDefs.add(new StepDef(OrderConstants.STATUS_AWAITING_CONFIRM,  "Đơn hàng đã đặt"));
        stepDefs.add(new StepDef(OrderConstants.STATUS_AWAITING_SHIPMENT, "Đã xác nhận"));
        stepDefs.add(new StepDef(OrderConstants.STATUS_AWAITING_DELIVERY, "Đã giao cho ĐVVC"));
        if (cancelled) {
            stepDefs.add(new StepDef(OrderConstants.STATUS_CANCELLED, "Đã hủy"));
        } else {
            stepDefs.add(new StepDef(OrderConstants.STATUS_COMPLETED, "Hoàn thành"));
            stepDefs.add(new StepDef(0, "Đánh giá")); // virtual step, statusCode=0
            if (hasReturn) {
                // Bước "Trả hàng" — chỉ thêm khi khách đã yêu cầu trả hàng/hoàn tiền.
                stepDefs.add(new StepDef(RETURN_STEP_CODE, "Trả hàng"));
            }
        }

        // ── Build danh sách bước ───────────────────────────────────────────────
        List<TimelineStep> steps = new ArrayList<>();
        for (int i = 0; i < stepDefs.size(); i++) {
            StepDef def = stepDefs.get(i);
            int code = def.statusCode();

            // Bước đầu tiên (status 1) luôn completed, dùng createdDate của đơn
            boolean completed;
            boolean current;
            Date timestamp = null;
            Long updatedByUserId = null;
            String updatedByUsername = null;
            String updatedByFullName = null;
            String note = null;

            if (code == OrderConstants.STATUS_AWAITING_CONFIRM) {
                // Bước khởi tạo – luôn hoàn thành
                completed = true;
                current   = (currentStatus == OrderConstants.STATUS_AWAITING_CONFIRM);
                timestamp = order.getCreatedDate();
            } else if (code == 0) {
                // Bước virtual "Đánh giá" – chỉ hiển thị sau khi hoàn thành
                completed = false;
                current   = false;
                timestamp = null;
            } else if (code == RETURN_STEP_CODE) {
                // Bước "Trả hàng" – đã có yêu cầu nên coi như đã đạt; ghi trạng thái chi tiết vào note.
                completed = true;
                current   = order.getReturnRefundStatus() != null
                        && order.getReturnRefundStatus() != OrderConstants.RETURN_STATUS_REFUNDED
                        && order.getReturnRefundStatus() != OrderConstants.RETURN_STATUS_REJECTED;
                timestamp = latestReturnEntry != null ? latestReturnEntry.getCreatedAt() : order.getModifiedDate();
                note = returnStatusLabel(order.getReturnRefundStatus());
                if (latestReturnEntry != null && latestReturnEntry.getChangedByUser() != null) {
                    updatedByUserId   = latestReturnEntry.getChangedByUser().getId();
                    updatedByUsername = latestReturnEntry.getChangedByUsername();
                    if (latestReturnEntry.getChangedByUser().getUserInfo() != null) {
                        updatedByFullName = latestReturnEntry.getChangedByUser().getUserInfo().getFullName();
                    }
                } else if (latestReturnEntry != null) {
                    updatedByUsername = latestReturnEntry.getChangedByUsername();
                }
            } else {
                OrderHistoryEntity entry = firstEntryByStatus.get(code);
                if (entry != null) {
                    completed = true;
                    current   = (currentStatus == code);
                    timestamp = entry.getCreatedAt();
                    if (entry.getChangedByUser() != null) {
                        updatedByUserId   = entry.getChangedByUser().getId();
                        updatedByUsername = entry.getChangedByUsername();
                        if (entry.getChangedByUser().getUserInfo() != null) {
                            updatedByFullName = entry.getChangedByUser().getUserInfo().getFullName();
                        }
                    } else {
                        updatedByUsername = entry.getChangedByUsername();
                    }
                    note = entry.getNote();
                } else {
                    // Chưa đạt tới bước này
                    completed = false;
                    current   = false;
                }
            }

            steps.add(TimelineStep.builder()
                    .stepIndex(i + 1)
                    .statusCode(code <= 0 ? null : code)
                    .statusLabel(def.label())
                    .completed(completed)
                    .current(current)
                    .timestamp(timestamp)
                    .updatedByUserId(updatedByUserId)
                    .updatedByUsername(updatedByUsername)
                    .updatedByFullName(updatedByFullName)
                    .note(note)
                    .build());
        }

        return OrderTimelineResponse.builder()
                .orderCode(order.getOrderCode())
                .currentStatus(currentStatus)
                .currentStatusLabel(orderStatusLabel(currentStatus))
                .finished(currentStatus == OrderConstants.STATUS_COMPLETED
                        || currentStatus == OrderConstants.STATUS_CANCELLED)
                .steps(steps)
                .build();
    }

    @Override
    @Transactional
    public OrderResponse requestReturn(Long orderId, OrderReturnRequest request,
                                       List<org.springframework.web.multipart.MultipartFile> mediaFiles) {
        UserEntity currentUser = userService.getCurrentUser();
        OrderEntity order = orderRepository.findByIdAndUser_Id(orderId, currentUser.getId())
                .orElseThrow(() -> new NotFoundEntityException("Order not found with id: " + orderId));

        if (order.getStatus() == null
                || order.getStatus() != OrderConstants.STATUS_COMPLETED) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Return / refund is only available for completed orders");
        }
        if (order.getReturnRefundStatus() != null) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Return or refund request already exists for this order");
        }
        // Ưu tiên completedAt; fallback sang modifiedDate cho đơn cũ chưa có completedAt.
        Date deliveredAt = order.getCompletedAt() != null
                ? order.getCompletedAt()
                : order.getModifiedDate();
        if (!isWithinReturnWindowAfterDelivered(deliveredAt)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Return / refund is only available within "
                            + OrderConstants.RETURN_ELIGIBLE_DAYS_AFTER_PAID
                            + " days after delivery");
        }

        String note = buildReturnNote(request);
        Integer oldReturnRefundStatus = order.getReturnRefundStatus();
        order.setReturnRefundStatus(OrderConstants.RETURN_STATUS_REQUESTED);
        order.setReturnRefundNote(note);
        order = orderRepository.save(order);

        // Upload ảnh / video bằng chứng (nếu có) lên Cloudinary và lưu media records.
        saveReturnMedia(order, mediaFiles);

        orderHistoryService.logReturnRefundStatusChange(
                order, oldReturnRefundStatus, OrderConstants.RETURN_STATUS_REQUESTED,
                "Khách hàng yêu cầu trả hàng / hoàn tiền: " + note);

        eventPublisher.publishEvent(new OrderReturnedEvent(this, order.getId()));

        List<OrderDetailEntity> details = orderDetailRepository.findByOrder_IdOrderByIdAsc(order.getId());
        return buildOrderResponse(order, mapDetailResponses(details));
    }

    @Override
    @Transactional
    public OrderResponse cancelMyOrder(Long orderId, String cancelReason) {
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
        Integer oldStatusBeforeCancel = order.getStatus();
        order.setStatus(OrderConstants.STATUS_CANCELLED);
        order.setCancelledBy("CUSTOMER");
        if (cancelReason != null && !cancelReason.isBlank()) {
            order.setCancelNote(cancelReason.trim());
        }
        order = orderRepository.save(order);

        String historyNote = "Khách hàng tự hủy đơn hàng"
                + (cancelReason != null && !cancelReason.isBlank()
                    ? ". Lý do: " + cancelReason.trim()
                    : "");
        orderHistoryService.logOrderStatusChange(
                order, oldStatusBeforeCancel, OrderConstants.STATUS_CANCELLED, historyNote);

        // Tự động hủy task Kanban tương ứng (nếu có)
        eventPublisher.publishEvent(new OrderCancelledEvent(this, order.getId()));

        List<OrderDetailEntity> details = orderDetailRepository.findByOrder_IdOrderByIdAsc(order.getId());
        // Đồng bộ kho: nhả hàng đã giữ khi khách hủy đơn.
        inventoryService.releaseForOrder(details);
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

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> adminGetAllOrders(Integer status) {
        List<OrderEntity> orders = (status != null)
                ? orderRepository.findByStatusOrderByIdDesc(status)
                : orderRepository.findAllByOrderByIdDesc();
        return orders.stream().map(o -> {
            List<OrderDetailEntity> details = orderDetailRepository.findByOrder_IdOrderByIdAsc(o.getId());
            return buildOrderResponse(o, mapDetailResponses(details));
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse adminGetOrderById(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy đơn hàng id=" + orderId));
        List<OrderDetailEntity> details = orderDetailRepository.findByOrder_IdOrderByIdAsc(orderId);
        return buildOrderResponse(order, mapDetailResponses(details));
    }

    @Override
    @Transactional
    public OrderResponse adminUpdateOrderStatus(Long orderId, Integer newStatus, String cancelNote) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy đơn hàng id=" + orderId));

        int current = order.getStatus() != null ? order.getStatus() : 0;

        if (current == OrderConstants.STATUS_COMPLETED || current == OrderConstants.STATUS_CANCELLED) {
            throw new CustomApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Đơn hàng đã ở trạng thái '" + orderStatusLabel(current) + "' — không thể cập nhật.");
        }

        if (!isValidAdminTransition(current, newStatus)) {
            throw new CustomApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Không thể chuyển trạng thái từ '" + orderStatusLabel(current)
                            + "' sang '" + orderStatusLabel(newStatus) + "'.");
        }

        // Bắt buộc lý do khi admin hủy đơn
        if (newStatus == OrderConstants.STATUS_CANCELLED
                && (cancelNote == null || cancelNote.isBlank())) {
            throw new CustomApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Vui lòng cung cấp lý do hủy đơn hàng.");
        }

        order.setStatus(newStatus);
        if (newStatus == OrderConstants.STATUS_COMPLETED) {
            order.setCompletedAt(new Date());
            // Đơn hoàn thành => coi như đã thu tiền (đặc biệt COD). Không ghi đè nếu đã thanh toán trước đó (VNPAY).
            if (!Boolean.TRUE.equals(order.getPaid())) {
                order.setPaid(true);
                order.setPaidAt(new Date());
            }
        }
        if (newStatus == OrderConstants.STATUS_CANCELLED) {
            order.setCancelledBy("ADMIN");
            if (cancelNote != null && !cancelNote.isBlank()) {
                order.setCancelNote(cancelNote.trim());
            }
        }
        order = orderRepository.save(order);
        log.info("Admin updated order status: orderId={}, {} → {}", orderId, current, newStatus);

        String historyNote = "Admin cập nhật: " + orderStatusLabel(current) + " → " + orderStatusLabel(newStatus)
                + (newStatus == OrderConstants.STATUS_CANCELLED && cancelNote != null && !cancelNote.isBlank()
                    ? ". Lý do: " + cancelNote.trim()
                    : "");
        orderHistoryService.logOrderStatusChange(order, current, newStatus, historyNote);

        // Thông báo cho khách hàng về thay đổi trạng thái đơn.
        notificationService.notifyUser(
                order.getUser(),
                "ORDER_STATUS",
                "Cập nhật đơn hàng " + order.getOrderCode(),
                "Đơn hàng của bạn đã chuyển sang trạng thái: " + orderStatusLabel(newStatus)
                        + (newStatus == OrderConstants.STATUS_CANCELLED && cancelNote != null && !cancelNote.isBlank()
                                ? ". Lý do: " + cancelNote.trim() : ""),
                order.getId());

        // Khi admin hủy đơn → tự động chuyển task liên kết sang CANCELLED
        if (newStatus == OrderConstants.STATUS_CANCELLED) {
            eventPublisher.publishEvent(new OrderCancelledEvent(this, order.getId()));
        }

        List<OrderDetailEntity> details = orderDetailRepository.findByOrder_IdOrderByIdAsc(orderId);
        // Đồng bộ kho theo trạng thái mới.
        if (newStatus == OrderConstants.STATUS_COMPLETED) {
            // Xuất kho thật + cộng soldCount khi đơn hoàn thành.
            increaseSoldCount(details);
            inventoryService.commitSaleForOrder(details);
        } else if (newStatus == OrderConstants.STATUS_CANCELLED) {
            // Nhả hàng đã giữ khi admin hủy đơn.
            inventoryService.releaseForOrder(details);
        }
        return buildOrderResponse(order, mapDetailResponses(details));
    }

    /** Cộng soldCount cho từng sản phẩm khi đơn hoàn thành (atomic, idempotency dựa trên việc gọi 1 lần ở COMPLETED). */
    private void increaseSoldCount(List<OrderDetailEntity> details) {
        if (details == null) return;
        for (OrderDetailEntity d : details) {
            int qty = d.getQuantity() != null ? d.getQuantity() : 0;
            if (qty <= 0 || d.getProduct() == null || d.getProduct().getId() == null) continue;
            productRepository.incrementSoldCount(d.getProduct().getId(), qty);
        }
    }

    @Override
    @Transactional
    public OrderResponse adminUpdateReturnStatus(Long orderId, Integer newReturnStatus, String note, boolean restockToSellable) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy đơn hàng id=" + orderId));

        Integer currentReturnStatus = order.getReturnRefundStatus();
        if (currentReturnStatus == null || currentReturnStatus == OrderConstants.RETURN_STATUS_NONE) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Đơn hàng này chưa có yêu cầu trả hàng.");
        }
        if (currentReturnStatus == OrderConstants.RETURN_STATUS_REFUNDED
                || currentReturnStatus == OrderConstants.RETURN_STATUS_REJECTED) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Yêu cầu trả hàng đã được xử lý xong — không thể cập nhật thêm.");
        }
        if (!isValidAdminReturnTransition(currentReturnStatus, newReturnStatus)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "Không thể chuyển trạng thái trả hàng từ '"
                            + returnStatusLabel(currentReturnStatus) + "' sang '"
                            + returnStatusLabel(newReturnStatus) + "'.");
        }

        order.setReturnRefundStatus(newReturnStatus);
        // KHÔNG ghi đè returnRefundNote (lý do gốc của khách). Ghi chú admin chỉ lưu vào lịch sử bên dưới.
        order = orderRepository.save(order);
        log.info("Admin updated return status: orderId={}, {} -> {}", orderId, currentReturnStatus, newReturnStatus);

        orderHistoryService.logReturnRefundStatusChange(
                order, currentReturnStatus, newReturnStatus,
                "Admin cập nhật trả hàng: " + returnStatusLabel(currentReturnStatus)
                        + " -> " + returnStatusLabel(newReturnStatus)
                        + (note != null && !note.isBlank() ? ". Ghi chú: " + note : ""));

        // Thông báo cho khách hàng về cập nhật trả hàng / hoàn tiền.
        notificationService.notifyUser(
                order.getUser(),
                "RETURN_REFUND",
                "Cập nhật trả hàng đơn " + order.getOrderCode(),
                "Yêu cầu trả hàng/hoàn tiền của bạn: " + returnStatusLabel(newReturnStatus)
                        + (note != null && !note.isBlank() ? ". Ghi chú: " + note : ""),
                order.getId());

        List<OrderDetailEntity> details = orderDetailRepository.findByOrder_IdOrderByIdAsc(orderId);
        // Đồng bộ kho: khi đã HOÀN TIỀN → nhập lại kho (nếu hàng tốt) và trừ soldCount.
        if (newReturnStatus == OrderConstants.RETURN_STATUS_REFUNDED) {
            inventoryService.restockForOrder(details, restockToSellable);
            decreaseSoldCount(details);
        }
        return buildOrderResponse(order, mapDetailResponses(details));
    }

    /** Trừ soldCount khi đơn được hoàn tiền (đảo lại lượng bán). */
    private void decreaseSoldCount(List<OrderDetailEntity> details) {
        if (details == null) return;
        for (OrderDetailEntity d : details) {
            int qty = d.getQuantity() != null ? d.getQuantity() : 0;
            if (qty <= 0 || d.getProduct() == null || d.getProduct().getId() == null) continue;
            productRepository.decrementSoldCount(d.getProduct().getId(), qty);
        }
    }

    private static boolean isValidAdminTransition(int from, int to) {
        switch (from) {
            case OrderConstants.STATUS_AWAITING_CONFIRM:
                // Chỉ được hủy khi đơn còn ở "Chờ chuẩn bị".
                return to == OrderConstants.STATUS_AWAITING_SHIPMENT || to == OrderConstants.STATUS_CANCELLED;
            case OrderConstants.STATUS_AWAITING_SHIPMENT:
                return to == OrderConstants.STATUS_AWAITING_DELIVERY;
            case OrderConstants.STATUS_AWAITING_DELIVERY:
                return to == OrderConstants.STATUS_COMPLETED;
            default:
                return false;
        }
    }

    private static String orderStatusLabel(int status) {
        switch (status) {
            case 1: return "Chờ chuẩn bị";
            case 2: return "Chờ vận chuyển";
            case 3: return "Chờ giao hàng";
            case 4: return "Hoàn thành";
            case 5: return "Đã hủy";
            default: return "Không xác định (" + status + ")";
        }
    }

    private static boolean isWithinReturnWindowAfterDelivered(Date completedAt) {
        if (completedAt == null) {
            return false;
        }
        Instant p = completedAt.toInstant();
        Instant end = p.plus(OrderConstants.RETURN_ELIGIBLE_DAYS_AFTER_PAID, ChronoUnit.DAYS);
        return !Instant.now().isAfter(end);
    }

    private static boolean isValidAdminReturnTransition(int from, int to) {
        switch (from) {
            case OrderConstants.RETURN_STATUS_REQUESTED:
                return to == OrderConstants.RETURN_STATUS_ACCEPTED || to == OrderConstants.RETURN_STATUS_REJECTED;
            case OrderConstants.RETURN_STATUS_ACCEPTED:
                return to == OrderConstants.RETURN_STATUS_REFUNDING || to == OrderConstants.RETURN_STATUS_REJECTED;
            case OrderConstants.RETURN_STATUS_REFUNDING:
                return to == OrderConstants.RETURN_STATUS_REFUNDED || to == OrderConstants.RETURN_STATUS_REJECTED;
            default:
                return false;
        }
    }

    private static String returnStatusLabel(int status) {
        switch (status) {
            case 1: return "Yêu cầu trả hàng";
            case 2: return "Đã chấp nhận";
            case 3: return "Đang hoàn tiền";
            case 4: return "Hoàn tiền xong";
            case 5: return "Từ chối";
            default: return "Không xác định (" + status + ")";
        }
    }

    /** Ghép note từ các trường trong request thành chuỗi lưu DB. */
    private static String buildReturnNote(OrderReturnRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.getReason() != null && !request.getReason().isBlank()) {
            sb.append(request.getReason());
        }
        if (request.getRefundMethod() != null && !request.getRefundMethod().isBlank()) {
            sb.append(" | refundMethod=").append(request.getRefundMethod());
        }
        if (request.getBankName() != null && !request.getBankName().isBlank()) {
            sb.append(" | bank=").append(request.getBankName());
        }
        if (request.getBankAccountNumber() != null && !request.getBankAccountNumber().isBlank()) {
            sb.append(" | acct=").append(request.getBankAccountNumber());
        }
        if (request.getBankEmail() != null && !request.getBankEmail().isBlank()) {
            sb.append(" | email=").append(request.getBankEmail());
        }
        return sb.toString().trim();
    }

    /**
     * Xác định ai hủy đơn.
     * - Ưu tiên field `cancelledBy` (đã lưu với đơn mới).
     * - Fallback: tra order_history → kiểm tra role của người thực hiện hủy.
     *   · Nếu CUSTOMER role → "CUSTOMER"
     *   · Ngược lại (employee, manager, admin, …) → "ADMIN"
     */
    private String resolveCancelledBy(OrderEntity order) {
        if (order.getCancelledBy() != null) return order.getCancelledBy();
        if (!Integer.valueOf(OrderConstants.STATUS_CANCELLED).equals(order.getStatus())) return null;

        // Tra lịch sử (kèm fetch changedByUser + role để tránh N+1)
        List<com.ndh.ShopTechnology.entities.log.OrderHistoryEntity> history =
                orderHistoryRepository.findByOrderIdAndChangeTypeWithActorOrderByCreatedAtDesc(
                        order.getId(),
                        com.ndh.ShopTechnology.entities.log.OrderHistoryEntity.CHANGE_TYPE_ORDER_STATUS);

        return history.stream()
                .filter(h -> Integer.valueOf(OrderConstants.STATUS_CANCELLED).equals(h.getNewStatus()))
                .findFirst()
                .map(h -> {
                    // Nếu không có user gắn vào entry → coi là CUSTOMER (self-cancel cũ)
                    if (h.getChangedByUser() == null) return "CUSTOMER";
                    // Kiểm tra role của người thực hiện
                    boolean isCustomer = h.getChangedByUser().hasRole(
                            com.ndh.ShopTechnology.constants.RoleConstant.ROLE_CUSTOMER);
                    return isCustomer ? "CUSTOMER" : "ADMIN";
                })
                .orElse(null);
    }

    private OrderResponse buildOrderResponse(OrderEntity order, List<OrderDetailResponse> orderDetails) {
        PaymentMethodEntity pm = order.getPaymentMethod();
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .returnRefundStatus(order.getReturnRefundStatus())
                .returnRefundNote(order.getReturnRefundNote())
                .cancelNote(order.getCancelNote())
                .cancelledBy(resolveCancelledBy(order))
                .description(order.getDescription())
                .total(order.getTotal())
                .typeOrder(order.getTypeOrder())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryDistanceMeters(order.getDeliveryDistanceMeters())
                .shippingFeeVnd(order.getShippingFeeVnd())
                .paid(order.getPaid())
                .paidAt(order.getPaidAt())
                .completedAt(order.getCompletedAt())
                .paymentMethod(pm == null ? null : PaymentMethodSummaryResponse.builder()
                        .id(pm.getId())
                        .name(pm.getName())
                        .code(pm.getCode())
                        .build())
                .orderDetails(orderDetails)
                .returnMedia(loadReturnMedia(order.getId()))
                .createdDate(order.getCreatedDate())
                .modifiedDate(order.getModifiedDate())
                .build();
    }

    @Override
    @Transactional
    public OrderResponse adminDeleteReturnMedia(Long orderId, Long mediaId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy đơn hàng id=" + orderId));
        com.ndh.ShopTechnology.entities.doc.DocumentEntity doc =
                documentRepository.findById(mediaId)
                        .orElseThrow(() -> new NotFoundEntityException("Không tìm thấy media id=" + mediaId));
        boolean belongs = doc.getEntityId() != null
                && orderId.equals(doc.getEntityId())
                && doc.getEntityType() != null
                && doc.getEntityType() == com.ndh.ShopTechnology.constants.DocumentEntityType.ID_DOCUMENT_ENTITY_ORDER;
        if (!belongs) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Media không thuộc đơn hàng này");
        }
        // Xoá asset trên Cloudinary (resource_type theo loại media) rồi xoá bản ghi document.
        String resourceType = doc.getType() == com.ndh.ShopTechnology.constants.DocumentKind.VIDEO ? "video" : "image";
        cloudinaryService.deleteByPublicId(doc.getCloudinaryPublicId(), resourceType);
        documentRepository.delete(doc);

        List<OrderDetailEntity> details = orderDetailRepository.findByOrder_IdOrderByIdAsc(orderId);
        return buildOrderResponse(order, mapDetailResponses(details));
    }

    /** Upload và lưu ảnh / video bằng chứng trả hàng. Bỏ qua file rỗng / null. */
    private void saveReturnMedia(OrderEntity order,
                                 List<org.springframework.web.multipart.MultipartFile> mediaFiles) {
        if (mediaFiles == null || mediaFiles.isEmpty()) return;
        for (org.springframework.web.multipart.MultipartFile file : mediaFiles) {
            if (file == null || file.isEmpty()) continue;
            com.ndh.ShopTechnology.services.storage.CloudinaryService.ReturnMediaUploadResult up =
                    cloudinaryService.uploadReturnMedia(file, order.getId());
            int kind = com.ndh.ShopTechnology.constants.DocumentKind.resolve(file);
            com.ndh.ShopTechnology.entities.doc.DocumentEntity doc =
                    com.ndh.ShopTechnology.entities.doc.DocumentEntity.builder()
                            .fileName(file.getOriginalFilename())
                            .fileSize(String.valueOf(file.getSize()))
                            .filePath(up.url())
                            .cloudinaryPublicId(up.publicId())
                            .type(kind)
                            .main(false)
                            .entityId(order.getId())
                            .entityType(com.ndh.ShopTechnology.constants.DocumentEntityType.ID_DOCUMENT_ENTITY_ORDER)
                            .build();
            documentRepository.save(doc);
        }
    }

    /** Lấy danh sách media trả hàng của đơn; trả null nếu không có (JsonInclude.NON_NULL sẽ ẩn field). */
    private List<com.ndh.ShopTechnology.dto.response.order.OrderReturnMediaResponse> loadReturnMedia(Long orderId) {
        List<com.ndh.ShopTechnology.entities.doc.DocumentEntity> rows =
                documentRepository.findAllByEntityIdAndEntityType(
                        orderId, com.ndh.ShopTechnology.constants.DocumentEntityType.ID_DOCUMENT_ENTITY_ORDER);
        if (rows == null || rows.isEmpty()) return null;
        return rows.stream()
                .map(d -> com.ndh.ShopTechnology.dto.response.order.OrderReturnMediaResponse.builder()
                        .id(d.getId())
                        .url(d.getFilePath())
                        .type(d.getType() == com.ndh.ShopTechnology.constants.DocumentKind.VIDEO ? "VIDEO" : "IMAGE")
                        .build())
                .toList();
    }

    private List<OrderDetailResponse> mapDetailResponses(List<OrderDetailEntity> details) {
        if (details == null || details.isEmpty()) return List.of();
        // Batch-fetch thumbnail URLs cho tất cả productId trong list
        List<Long> productIds = details.stream()
                .map(d -> d.getProduct() != null ? d.getProduct().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> thumbnailByProductId =
                productImageAttachService.getPrimaryImageUrlsByProductIds(productIds);

        return details.stream()
                .map(d -> {
                    OrderLinePricingProgramsDto programs = null;
                    String raw = d.getPricingProgramsJson();
                    if (raw != null && !raw.isBlank()) {
                        try {
                            programs = objectMapper.readValue(raw, OrderLinePricingProgramsDto.class);
                        } catch (Exception e) {
                            log.warn("order_detail id={}: could not parse pricing_programs_json", d.getId(), e);
                        }
                    }
                    Long productId = d.getProduct().getId();
                    OrderDetailResponse.OrderDetailResponseBuilder b = OrderDetailResponse.builder()
                            .id(d.getId())
                            .productId(productId)
                            .productName(d.getProduct().getProductName())
                            .thumbnailUrl(thumbnailByProductId.get(productId));
                    ProductVariantEntity pv = d.getProductVariant();
                    if (pv != null) {
                        b.productVariantId(pv.getId())
                         .variantSkuCode(pv.getSkuCode())
                         .variantOptions(pv.getOptionValues());
                    }
                    return b.quantity(d.getQuantity())
                            .unitPrice(d.getUnitPrice())
                            .lineTotal(d.getTotalPrice())
                            .description(d.getDescription())
                            .pricingPrograms(programs)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
