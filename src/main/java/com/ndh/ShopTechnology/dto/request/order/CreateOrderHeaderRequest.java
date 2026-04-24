package com.ndh.ShopTechnology.dto.request.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
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
     * Địa chỉ giao hàng lưu dạng text (snapshot; không lưu id user_address).
     */
    @NotBlank(message = "deliveryAddress is required")
    private String deliveryAddress;

    @NotNull(message = "paymentMethodId is required")
    private Long paymentMethodId;
}
