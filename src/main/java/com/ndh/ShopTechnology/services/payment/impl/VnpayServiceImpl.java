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
import com.ndh.ShopTechnology.services.payment.VnpayQueryDrResponse;
import com.ndh.ShopTechnology.services.payment.VnpayQueryDrService;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.services.user.UserService;
import com.ndh.ShopTechnology.util.VnpayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VnpayServiceImpl implements VnpayService {

    private static final Logger log = LoggerFactory.getLogger(VnpayServiceImpl.class);
    private static final DateTimeFormatter VNP_DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String SUCCESS_RESPONSE = "00";
    private static final String SUCCESS_STATUS = "00";
    /** vnp_TransactionStatus = "02" => giao dịch lỗi (kết luận thất bại). */
    private static final String FAILED_STATUS = "02";
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VnpayProperties vnpayProperties;
    private final OrderRepository orderRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final OrderService orderService;
    private final UserService userService;
    private final VnpayQueryDrService queryDrService;

    public VnpayServiceImpl(
            VnpayProperties vnpayProperties,
            OrderRepository orderRepository,
            CheckoutSessionRepository checkoutSessionRepository,
            OrderService orderService,
            UserService userService,
            VnpayQueryDrService queryDrService) {
        this.vnpayProperties = vnpayProperties;
        this.orderRepository = orderRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.orderService = orderService;
        this.userService = userService;
        this.queryDrService = queryDrService;
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
        UserEntity currentUser = userService.getCurrentUser();
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

            CheckoutSessionEntity session = checkoutSessionRepository
                    .findByIdWithUserAndPaymentMethod(refId)
                    .orElse(null);
            if (session != null) {
                return handleIpnForCheckoutSession(p, session, vnpLong);
            }

            Optional<OrderEntity> orderByTxn = orderRepository.findByVnpayCheckoutTxnRef(refId);
            if (orderByTxn.isPresent()) {
                return handleIpnForOrderFromDeletedCheckout(p, orderByTxn.get(), vnpLong);
            }

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
    public void reconcilePendingCheckoutSession(long checkoutSessionId) {
        // Tải phiên (eager user + paymentMethod nhờ EntityGraph) — KHÔNG mở transaction quanh HTTP call.
        CheckoutSessionEntity session = checkoutSessionRepository
                .findByIdWithUserAndPaymentMethod(checkoutSessionId)
                .orElse(null);
        if (session == null || session.getStatus() != CheckoutSessionStatus.PENDING) {
            return;
        }
        if (session.getPaymentMethod() == null
                || !vnpayProperties.getPaymentMethodCode()
                        .equalsIgnoreCase(Objects.toString(session.getPaymentMethod().getCode(), ""))) {
            return;
        }
        if (session.getTotal() == null || session.getTotal() <= 0) {
            return;
        }
        // Quá hạn phiên thì để luồng hết hạn xử lý, không truy vấn nữa.
        if (session.getExpiresAt() != null && session.getExpiresAt().before(new Date())) {
            return;
        }

        String txnRef = String.valueOf(session.getId());
        VnpayQueryDrResponse r = queryDrService.query(txnRef, session.getCreatedDate(), "127.0.0.1");
        if (!r.callOk()) {
            return; // mạng/parse lỗi — thử lại ở chu kỳ sau
        }

        // vnp_ResponseCode của API truy vấn: "00" = truy vấn được; khác (vd "91" not found,
        // "94" duplicate do query quá dày) => chưa kết luận, không xét chữ ký, thử lại sau.
        if (!SUCCESS_RESPONSE.equals(r.responseCode())) {
            return;
        }

        // Đến đây response mới mang dữ liệu giao dịch -> kiểm tra chữ ký.
        if (vnpayProperties.isQueryDrRequireValidSignature() && !r.signatureValid()) {
            log.warn("VNPAY querydr response signature invalid for txnRef={}", txnRef);
            return;
        }
        if (!r.signatureValid()) {
            log.warn("VNPAY querydr response signature mismatch (tolerated) for txnRef={}", txnRef);
        }

        String transStatus = r.transactionStatus();
        if (SUCCESS_STATUS.equals(transStatus)) {
            long expectedVnp = Math.round(session.getTotal() * 100.0);
            if (r.amount() == null || r.amount() != expectedVnp) {
                log.warn("VNPAY querydr amount mismatch txnRef={} expected={} got={}",
                        txnRef, expectedVnp, r.amount());
                return; // không finalize nếu số tiền không khớp
            }
            VnpaySessionFinalizeResult fin = orderService.finalizeVnpayCheckoutSession(session.getId());
            log.info("VNPAY querydr reconciled SUCCESS txnRef={} -> {}", txnRef, fin);
            return;
        }
        if (FAILED_STATUS.equals(transStatus)) {
            // Tải lại để tránh ghi đè nếu IPN vừa hoàn tất phiên này ở luồng khác.
            CheckoutSessionEntity fresh = checkoutSessionRepository.findById(session.getId()).orElse(null);
            if (fresh != null && fresh.getStatus() == CheckoutSessionStatus.PENDING) {
                fresh.setStatus(CheckoutSessionStatus.FAILED);
                checkoutSessionRepository.save(fresh);
                log.info("VNPAY querydr reconciled FAILED txnRef={}", txnRef);
            }
        }
        // transStatus "01" (chưa hoàn tất) hoặc khác => giữ PENDING, thử lại chu kỳ sau.
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
