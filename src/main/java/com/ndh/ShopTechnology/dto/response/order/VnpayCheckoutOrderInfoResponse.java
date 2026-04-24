package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Thông tin đơn hàng lấy theo {@code public_id} (phiên VNPAY): snapshot trước khi tạo bản ghi
 * {@code orders}, hoặc tóm tắt từ đơn thật khi {@code state = COMPLETED}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VnpayCheckoutOrderInfoResponse {

    /** Có khi đã tạo đơn thật. */
    private Long orderId;
    private String orderCode;
    private String description;
    private Integer typeOrder;
    private String deliveryAddress;
    private Double total;
    private List<OrderDetailResponse> orderDetails;
}
