package com.ndh.ShopTechnology.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mã hóa và chuỗi ký theo tài liệu VNPAY (HMAC-SHA512, sắp xếp tham số tăng dần).
 */
public final class VnpayUtils {

    private VnpayUtils() {
    }

    public static String hmacSha512(String secret, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec key = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(key);
            byte[] result = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(result);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA512 failed", e);
        }
    }

    public static String buildSignedQuery(Map<String, String> params, String hashSecret) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = params.get(fieldName);
            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }
            if (hashData.length() > 0) {
                hashData.append('&');
            }
            hashData.append(fieldName);
            hashData.append('=');
            hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

            if (query.length() > 0) {
                query.append('&');
            }
            query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
            query.append('=');
            query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
        }
        String secureHash = hmacSha512(hashSecret, hashData.toString());
        query.append("&vnp_SecureHash=");
        query.append(secureHash);
        return query.toString();
    }

    /**
     * Xác thực chữ ký callback (IPN / Return): cùng quy tắc tạo chuỗi, so khớp v_secureHash từ VNPAY.
     */
    public static boolean verifySecureHash(
            String hashSecret, Map<String, String> allParams, String vnpSecureHash) {
        if (vnpSecureHash == null || vnpSecureHash.isEmpty()) {
            return false;
        }
        Map<String, String> toSign = allParams.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .filter(e -> e.getKey().startsWith("vnp_")
                        && !"vnp_SecureHash".equals(e.getKey())
                        && !"vnp_SecureHashType".equals(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
        if (toSign.isEmpty()) {
            return false;
        }
        List<String> fieldNames = new ArrayList<>(toSign.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = toSign.get(fieldName);
            if (i > 0) {
                hashData.append('&');
            }
            hashData.append(fieldName);
            hashData.append('=');
            hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
        }
        String sign = hmacSha512(hashSecret, hashData.toString());
        return sign.equalsIgnoreCase(vnpSecureHash);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
