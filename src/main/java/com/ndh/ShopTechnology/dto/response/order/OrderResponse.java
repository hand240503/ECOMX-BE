package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    private Long id;
    private String orderCode;
    private Integer status;
    private Integer returnRefundStatus;
    private String returnRefundNote;
    private String description;
    private Double total;
    private Integer typeOrder;
    private String deliveryAddress;
    private Double deliveryDistanceMeters;
    private Long shippingFeeVnd;
    private Boolean paid;
    private Date paidAt;
    private PaymentMethodSummaryResponse paymentMethod;
    private List<OrderDetailResponse> orderDetails;
    private Date createdDate;
    private Date modifiedDate;
}
