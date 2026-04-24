package com.ndh.ShopTechnology.dto.request.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Body gửi từ FE: 1 bản ghi thông tin đơn + danh sách đơn chi tiết
 * → ứng với 1 {@code OrderEntity} và nhiều {@code OrderDetailEntity}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateOrderRequest {

    @NotNull(message = "order is required")
    @Valid
    private CreateOrderHeaderRequest order;

    @NotEmpty(message = "orderDetails must not be empty")
    @Valid
    private List<CreateOrderDetailRequest> orderDetails;
}
