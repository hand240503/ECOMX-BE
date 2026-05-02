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
    /** Mã hiển thị (VD DH-2026-00000001). */
    private String orderCode;
    private Integer status;
    /**
     * Trả hàng / hoàn tiền: null = chưa yêu cầu; 1–5 theo {@code OrderConstants} (return).
     */
    private Integer returnRefundStatus;
    private String returnRefundNote;
    private String description;
    private Double total;
    private Integer typeOrder;
    /** Địa chỉ giao hàng lưu snapshot. */
    private String deliveryAddress;
    /** Quãng đường lái tới kho lúc đặt (m), null nếu không tính được. */
    private Double deliveryDistanceMeters;
    /** Phí ship snapshot (VND). */
    private Long shippingFeeVnd;
    /** Đã ghi nhận thanh toán. */
    private Boolean paid;
    private Date paidAt;
    private PaymentMethodSummaryResponse paymentMethod;
    private List<OrderDetailResponse> orderDetails;
    private Date createdDate;
    private Date modifiedDate;
}
