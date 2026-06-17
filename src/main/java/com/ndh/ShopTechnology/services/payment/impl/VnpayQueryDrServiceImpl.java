package com.ndh.ShopTechnology.services.payment.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndh.ShopTechnology.config.VnpayProperties;
import com.ndh.ShopTechnology.services.payment.VnpayQueryDrResponse;
import com.ndh.ShopTechnology.services.payment.VnpayQueryDrService;
import com.ndh.ShopTechnology.util.VnpayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Triển khai API truy vấn giao dịch (querydr) của VNPAY (PAY 2.1.0).
 *
 * <p>Checksum của querydr KHÁC với checksum tạo URL thanh toán: dữ liệu ký là chuỗi
 * nối bằng dấu '|' theo đúng thứ tự tham số (không sort, không URL-encode).
 */
@Service
public class VnpayQueryDrServiceImpl implements VnpayQueryDrService {

    private static final Logger log = LoggerFactory.getLogger(VnpayQueryDrServiceImpl.class);
    private static final DateTimeFormatter VNP_DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VnpayProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public VnpayQueryDrServiceImpl(VnpayProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    @Override
    public VnpayQueryDrResponse query(String txnRef, Date transactionDate, String clientIp) {
        if (!StringUtils.hasText(props.getApiUrl())
                || !StringUtils.hasText(props.getHashSecret())
                || !StringUtils.hasText(props.getTmnCode())) {
            return VnpayQueryDrResponse.failedCall("VNPAY querydr not configured");
        }

        try {
            String requestId = UUID.randomUUID().toString().replace("-", "");
            String version = props.getVersion();
            String command = "querydr";
            String tmnCode = props.getTmnCode().trim();
            String orderInfo = "Truy van GD " + txnRef;
            String transDate = ZonedDateTime.ofInstant(
                    (transactionDate != null ? transactionDate : new Date()).toInstant(), VN).format(VNP_DATETIME);
            String createDate = ZonedDateTime.now(VN).format(VNP_DATETIME);
            String ip = StringUtils.hasText(clientIp) ? clientIp : "127.0.0.1";

            // Thứ tự ký bắt buộc của querydr (nối bằng '|', KHÔNG sort, KHÔNG encode)
            String hashData = String.join("|",
                    requestId, version, command, tmnCode, txnRef,
                    transDate, createDate, ip, orderInfo);
            String secureHash = VnpayUtils.hmacSha512(props.getHashSecret().trim(), hashData);

            Map<String, String> body = new LinkedHashMap<>();
            body.put("vnp_RequestId", requestId);
            body.put("vnp_Version", version);
            body.put("vnp_Command", command);
            body.put("vnp_TmnCode", tmnCode);
            body.put("vnp_TxnRef", txnRef);
            body.put("vnp_OrderInfo", orderInfo);
            body.put("vnp_TransactionDate", transDate);
            body.put("vnp_CreateDate", createDate);
            body.put("vnp_IpAddr", ip);
            body.put("vnp_SecureHash", secureHash);

            String json = objectMapper.writeValueAsString(body);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(props.getApiUrl().trim()))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("VNPAY querydr HTTP {} for txnRef={}", resp.statusCode(), txnRef);
                return VnpayQueryDrResponse.failedCall("HTTP " + resp.statusCode());
            }

            JsonNode node = objectMapper.readTree(resp.body());
            String responseCode = text(node, "vnp_ResponseCode");
            String message = text(node, "vnp_Message");
            String transactionStatus = text(node, "vnp_TransactionStatus");
            Long amount = parseLongOrNull(text(node, "vnp_Amount"));
            boolean signatureValid = verifyResponseSignature(node);

            log.info("VNPAY querydr resp txnRef={} transDate={} responseCode={} message=\"{}\" "
                            + "transactionStatus={} amount={} sigValid={}",
                    txnRef, transDate, responseCode, message, transactionStatus, amount, signatureValid);

            return new VnpayQueryDrResponse(true, responseCode, message, transactionStatus, amount, signatureValid);
        } catch (Exception e) {
            log.warn("VNPAY querydr call failed for txnRef={}: {}", txnRef, e.toString());
            return VnpayQueryDrResponse.failedCall(e.getMessage());
        }
    }

    /** Verify chữ ký phản hồi querydr theo thứ tự trường chuẩn 2.1.0 (nối '|'). */
    private boolean verifyResponseSignature(JsonNode node) {
        try {
            String received = text(node, "vnp_SecureHash");
            if (!StringUtils.hasText(received)) {
                return false;
            }
            String hashData = String.join("|",
                    nz(text(node, "vnp_ResponseId")),
                    nz(text(node, "vnp_Command")),
                    nz(text(node, "vnp_ResponseCode")),
                    nz(text(node, "vnp_Message")),
                    nz(text(node, "vnp_TmnCode")),
                    nz(text(node, "vnp_TxnRef")),
                    nz(text(node, "vnp_Amount")),
                    nz(text(node, "vnp_BankCode")),
                    nz(text(node, "vnp_PayDate")),
                    nz(text(node, "vnp_TransactionNo")),
                    nz(text(node, "vnp_TransactionType")),
                    nz(text(node, "vnp_TransactionStatus")),
                    nz(text(node, "vnp_OrderInfo")),
                    nz(text(node, "vnp_PromotionCode")),
                    nz(text(node, "vnp_PromotionAmount")));
            String computed = VnpayUtils.hmacSha512(props.getHashSecret().trim(), hashData);
            return computed.equalsIgnoreCase(received);
        } catch (Exception e) {
            return false;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : null;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static Long parseLongOrNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
