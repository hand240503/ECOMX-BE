package com.ndh.ShopTechnology.services.payment.impl;

import com.ndh.ShopTechnology.config.VnpayProperties;
import com.ndh.ShopTechnology.constants.OrderConstants;
import com.ndh.ShopTechnology.dto.response.payment.VnpayCreatePaymentData;
import com.ndh.ShopTechnology.dto.response.payment.VnpayIpnResponse;
import com.ndh.ShopTechnology.entities.order.CheckoutSessionEntity;
import com.ndh.ShopTechnology.entities.order.CheckoutSessionStatus;
import com.ndh.ShopTechnology.entities.order.OrderEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.CheckoutSessionRepository;
import com.ndh.ShopTechnology.repository.OrderRepository;
import com.ndh.ShopTechnology.services.order.OrderService;
import com.ndh.ShopTechnology.services.order.VnpaySessionFinalizeResult;
import com.ndh.ShopTechnology.services.payment.VnpayService;
import com.ndh.ShopTechnology.services.user.UserService;
import com.ndh.ShopTechnology.util.VnpayUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class VnpayServiceImpl implements VnpayService {

    private static final DateTimeFormatter VNP_DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String SUCCESS_RESPONSE = "00";
    private static final String SUCCESS_STATUS = "00";
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VnpayProperties vnpayProperties;
    private final OrderRepository orderRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final OrderService orderService;
    private final UserService userService;

    public VnpayServiceImpl(
            VnpayProperties vnpayProperties,
            OrderRepository orderRepository,
            CheckoutSessionRepository checkoutSessionRepository,
            OrderService orderService,
            UserService userService) {
        this.vnpayProperties = vnpayProperties;
        this.orderRepository = orderRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.orderService = orderService;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public VnpayCreatePaymentData createPaymentUrl(long checkoutSessionId, String clientIp) {
        if (!vnpayProperties.isEnabled() || !StringUtils.hasText(vnpayProperties.getHashSecret())) {
            throw new CustomApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "VNPAY is not configured (set vnpay.hash-secret / VNP_HASH_SECRET)");
        }
        if (!StringUtils.hasText(vnpayProperties.getTmnCode())) {
            throw new CustomApiException(HttpStatus.SERVICE_UNAVAILABLE, "VNPAY tmn code is not set");
        }
        var currentUser = userService.getCurrentUser();
        CheckoutSessionEntity session = checkoutSessionRepository
                .findByIdAndUser_IdAndStatus(checkoutSessionId, currentUser.getId(), CheckoutSessionStatus.PENDING)
                .orElseThrow(() -> new NotFoundEntityException(
                        "Checkout session not found or not pending: " + checkoutSessionId));

        if (session.getExpiresAt() != null && session.getExpiresAt().before(new Date())) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Checkout session has expired");
        }
        if (session.getPaymentMethod() == null
                || !vnpayProperties.getPaymentMethodCode()
                        .equalsIgnoreCase(Objects.toString(session.getPaymentMethod().getCode(), ""))) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST,
                    "This session is not using VNPAY. Choose payment method " + vnpayProperties.getPaymentMethodCode());
        }
        if (session.getTotal() == null || session.getTotal() <= 0) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }

        long vnpAmount = Math.round(session.getTotal() * 100.0);
        if (vnpAmount < 1) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Invalid VNPAY amount");
        }

        String txnRef = String.valueOf(session.getId());
        String orderInfo = "Thanh toan phien " + txnRef;
        if (orderInfo.length() > 255) {
            orderInfo = orderInfo.substring(0, 255);
        }

        ZonedDateTime now = ZonedDateTime.now(VN);
        String create = now.format(VNP_DATETIME);
        String expire = now.plusMinutes(vnpayProperties.getPaymentMinutesToExpire()).format(VNP_DATETIME);

        String ip = StringUtils.hasText(clientIp) ? clientIp : "127.0.0.1";
        if (ip.length() > 45) {
            ip = ip.substring(0, 45);
        }

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", vnpayProperties.getVersion());
        params.put("vnp_Command", vnpayProperties.getCommand());
        params.put("vnp_TmnCode", vnpayProperties.getTmnCode().trim());
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", vnpayProperties.getOrderType());
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl().trim());
        params.put("vnp_IpAddr", ip);
        params.put("vnp_CreateDate", create);
        params.put("vnp_ExpireDate", expire);

        String query = VnpayUtils.buildSignedQuery(params, vnpayProperties.getHashSecret().trim());
        String payUrl = vnpayProperties.getPayUrl().trim() + "?" + query;
        return VnpayCreatePaymentData.builder()
                .paymentUrl(payUrl)
                .txnRef(txnRef)
                .vnpAmount(vnpAmount)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VnpayIpnResponse handleIpn(HttpServletRequest request) {
        try {
            Map<String, String> p = toParamMap(request);
            String vnpHash = p.get("vnp_SecureHash");
            if (vnpHash == null) {
                return new VnpayIpnResponse("97", "Invalid Checksum");
            }
            if (!StringUtils.hasText(vnpayProperties.getHashSecret())) {
                return new VnpayIpnResponse("99", "VNPAY not configured");
            }
            if (!VnpayUtils.verifySecureHash(
                    vnpayProperties.getHashSecret().trim(), p, vnpHash.trim())) {
                return new VnpayIpnResponse("97", "Invalid Checksum");
            }

            String txnRef = p.get("vnp_TxnRef");
            if (!StringUtils.hasText(txnRef)) {
                return new VnpayIpnResponse("01", "Order not found");
            }
            long refId;
            try {
                refId = Long.parseLong(txnRef.trim());
            } catch (NumberFormatException e) {
                return new VnpayIpnResponse("01", "Order not found");
            }

            String amountStr = p.get("vnp_Amount");
            if (!StringUtils.hasText(amountStr)) {
                return new VnpayIpnResponse("04", "Invalid Amount");
            }
            long vnpLong;
            try {
                vnpLong = Long.parseLong(amountStr.trim());
            } catch (NumberFormatException e) {
                return new VnpayIpnResponse("04", "Invalid Amount");
            }

            // Ưu tiên: bản ghi phiên checkout (PENDING/…); sau thanh toán thành công vẫn còn dòng (COMPLETED + order_id)
            CheckoutSessionEntity session = checkoutSessionRepository
                    .findByIdWithUserAndPaymentMethod(refId)
                    .orElse(null);
            if (session != null) {
                return handleIpnForCheckoutSession(p, session, vnpLong);
            }

            // Dữ liệu cũ: phiên từng bị xóa sau tạo đơn — idempotency theo cùng vnp_TxnRef
            var orderByTxn = orderRepository.findByVnpayCheckoutTxnRef(refId);
            if (orderByTxn.isPresent()) {
                return handleIpnForOrderFromDeletedCheckout(p, orderByTxn.get(), vnpLong);
            }

            // Tương thích: đơn cũ tạo trước (đã có bản ghi order + chưa thanh toán)
            OrderEntity order = orderRepository.findById(refId).orElse(null);
            if (order == null) {
                return new VnpayIpnResponse("01", "Order not found");
            }
            return handleIpnForLegacyOrder(p, order, vnpLong);
        } catch (Exception e) {
            return new VnpayIpnResponse("99", "Unknow error");
        }
    }

    private VnpayIpnResponse handleIpnForCheckoutSession(
            Map<String, String> p, CheckoutSessionEntity session, long vnpLong) {
        if (session.getTotal() == null) {
            return new VnpayIpnResponse("04", "Invalid Amount");
        }
        long expectedVnp = Math.round(session.getTotal() * 100.0);
        if (vnpLong != expectedVnp) {
            return new VnpayIpnResponse("04", "Invalid Amount");
        }
        if (session.getPaymentMethod() == null
                || !vnpayProperties.getPaymentMethodCode()
                        .equalsIgnoreCase(Objects.toString(session.getPaymentMethod().getCode(), ""))) {
            return new VnpayIpnResponse("99", "Invalid order payment type");
        }

        if (session.getStatus() == CheckoutSessionStatus.COMPLETED) {
            return new VnpayIpnResponse("02", "Order already confirmed");
        }
        if (session.getStatus() == CheckoutSessionStatus.FAILED
                || session.getStatus() == CheckoutSessionStatus.EXPIRED
                || session.getStatus() == CheckoutSessionStatus.CANCELLED) {
            return new VnpayIpnResponse("02", "Order not in payable state");
        }
        if (session.getStatus() != CheckoutSessionStatus.PENDING) {
            return new VnpayIpnResponse("02", "Order not in payable state");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().before(new Date())) {
            session.setStatus(CheckoutSessionStatus.EXPIRED);
            checkoutSessionRepository.save(session);
            return new VnpayIpnResponse("99", "Session expired");
        }

        String responseCode = p.get("vnp_ResponseCode");
        String transStatus = p.get("vnp_TransactionStatus");
        if (SUCCESS_RESPONSE.equals(responseCode) && SUCCESS_STATUS.equals(transStatus)) {
            VnpaySessionFinalizeResult r = orderService.finalizeVnpayCheckoutSession(session.getId());
            if (r == VnpaySessionFinalizeResult.CREATED || r == VnpaySessionFinalizeResult.ALREADY_COMPLETED) {
                return new VnpayIpnResponse("00", "Confirm Success");
            }
            if (r == VnpaySessionFinalizeResult.NOT_FOUND) {
                return new VnpayIpnResponse("01", "Order not found");
            }
            if (r == VnpaySessionFinalizeResult.NOT_PAYABLE) {
                return new VnpayIpnResponse("99", "Session expired or not payable");
            }
            if (r == VnpaySessionFinalizeResult.BUSINESS_ERROR) {
                return new VnpayIpnResponse("99", "Unknow error");
            }
            return new VnpayIpnResponse("99", "Unknow error");
        }
        session.setStatus(CheckoutSessionStatus.FAILED);
        checkoutSessionRepository.save(session);
        return new VnpayIpnResponse("00", "Confirm Success");
    }

    private VnpayIpnResponse handleIpnForOrderFromDeletedCheckout(
            Map<String, String> p, OrderEntity order, long vnpLong) {
        if (order.getTotal() == null) {
            return new VnpayIpnResponse("04", "Invalid Amount");
        }
        long expectedVnp = Math.round(order.getTotal() * 100.0);
        if (vnpLong != expectedVnp) {
            return new VnpayIpnResponse("04", "Invalid Amount");
        }
        if (order.getPaymentMethod() == null
                || !vnpayProperties.getPaymentMethodCode()
                        .equalsIgnoreCase(Objects.toString(order.getPaymentMethod().getCode(), ""))) {
            return new VnpayIpnResponse("99", "Invalid order payment type");
        }
        String responseCode = p.get("vnp_ResponseCode");
        String transStatus = p.get("vnp_TransactionStatus");
        if (SUCCESS_RESPONSE.equals(responseCode) && SUCCESS_STATUS.equals(transStatus)) {
            if (Boolean.TRUE.equals(order.getPaid())) {
                return new VnpayIpnResponse("02", "Order already confirmed");
            }
            return new VnpayIpnResponse("99", "Unknow error");
        }
        return new VnpayIpnResponse("00", "Confirm Success");
    }

    private VnpayIpnResponse handleIpnForLegacyOrder(Map<String, String> p, OrderEntity order, long vnpLong) {
        if (order.getTotal() == null) {
            return new VnpayIpnResponse("04", "Invalid Amount");
        }
        long expectedVnp = Math.round(order.getTotal() * 100.0);
        if (vnpLong != expectedVnp) {
            return new VnpayIpnResponse("04", "Invalid Amount");
        }
        if (order.getPaymentMethod() == null
                || !vnpayProperties.getPaymentMethodCode()
                        .equalsIgnoreCase(Objects.toString(order.getPaymentMethod().getCode(), ""))) {
            return new VnpayIpnResponse("99", "Invalid order payment type");
        }

        if (Boolean.TRUE.equals(order.getPaid())) {
            return new VnpayIpnResponse("02", "Order already confirmed");
        }
        if (order.getStatus() != null && order.getStatus() == OrderConstants.STATUS_CANCELLED) {
            return new VnpayIpnResponse("02", "Order not in payable state");
        }

        String responseCode = p.get("vnp_ResponseCode");
        String transStatus = p.get("vnp_TransactionStatus");
        if (SUCCESS_RESPONSE.equals(responseCode) && SUCCESS_STATUS.equals(transStatus)) {
            order.setPaid(true);
            order.setPaidAt(new Date());
            orderRepository.save(order);
        }
        return new VnpayIpnResponse("00", "Confirm Success");
    }

    @Override
    public String buildReturnRedirectUrl(HttpServletRequest request) {
        String base = vnpayProperties.getFrontendRedirectBase();
        if (base == null) {
            base = "http://localhost:5173";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = vnpayProperties.getFrontendResultPath();
        if (path == null) {
            path = "";
        } else if (!path.isEmpty() && !path.startsWith("/")) {
            path = "/" + path;
        }

        String fullBase = base + path;
        Map<String, String> p = toParamMap(request);
        String vnpHash = p.get("vnp_SecureHash");
        if (vnpHash == null
                || !StringUtils.hasText(vnpayProperties.getHashSecret())
                || !VnpayUtils.verifySecureHash(
                        vnpayProperties.getHashSecret().trim(), p, vnpHash.trim())) {
            return fullBase + (fullBase.contains("?") ? "&" : "?") + "vnpay_verify=invalid";
        }
        String qs = request.getQueryString();
        if (StringUtils.hasText(qs)) {
            return fullBase + (fullBase.contains("?") ? "&" : "?") + qs;
        }
        return fullBase;
    }

    private static Map<String, String> toParamMap(HttpServletRequest request) {
        if (request.getParameterMap() == null) {
            return Collections.emptyMap();
        }
        return request.getParameterMap().entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().length > 0
                        && e.getValue()[0] != null
                        && !e.getValue()[0].isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0], (a, b) -> a, HashMap::new));
    }
}
