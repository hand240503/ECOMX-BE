package com.ndh.ShopTechnology.dto.request.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thông tin phần đầu đơn (bảng {@code orders}) — tách rõ với từng dòng {@link CreateOrderDetailRequest}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateOrderHeaderRequest {

    private String description;

    private Integer typeOrder;

    /**
     * Địa chỉ giao text (khi không dùng {@link #userAddressId}). Bắt buộc nếu chỉ gửi {@link #deliveryDistanceMeters}.
     */
    private String deliveryAddress;

    /**
     * Quãng đường từ FE (chỉ khi không dùng địa chỉ đã lưu, hoặc làm fallback khi địa chỉ lưu chưa có khoảng cách).
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "deliveryDistanceMeters must be >= 0")
    private Double deliveryDistanceMeters;

    /**
     * Ưu tiên: BE lấy {@code distance_to_warehouse_meters}, {@code shipping_fee_vnd} từ {@code user_address}
     * và ghép địa chỉ đầy đủ lưu vào {@code orders.delivery_address}.
     */
    private Long userAddressId;

    @NotNull(message = "paymentMethodId is required")
    private Long paymentMethodId;

    /**
     * Tùy chọn — chỉ ý nghĩa với VNPAY: id phiên làm việc do FE gán (vd. UUID), lưu vào {@code checkout_sessions.public_id}.
     * Phải duy nhất trong hệ thống. Bỏ trống thì BE gán {@code public_id} = chuỗi id bản ghi phiên.
     */
    private String checkoutWorkSessionId;
}
