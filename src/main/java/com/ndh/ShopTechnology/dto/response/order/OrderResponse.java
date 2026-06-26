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
    private String cancelNote;
    /** "CUSTOMER" hoặc "ADMIN" — chỉ có khi status=5 */
    private String cancelledBy;
    private String description;
    private Double total;
    private Integer typeOrder;
    private String deliveryAddress;
    private Double deliveryDistanceMeters;
    private Long shippingFeeVnd;
    private Long storeId;
    private String storeName;
    private Boolean paid;
    private Date paidAt;
    private Date completedAt;
    private PaymentMethodSummaryResponse paymentMethod;
    private List<OrderDetailResponse> orderDetails;
    /** Ảnh / video bằng chứng khách hàng gửi kèm yêu cầu trả hàng (rỗng nếu không có). */
    private List<OrderReturnMediaResponse> returnMedia;
    private Date createdDate;
    private Date modifiedDate;
}
