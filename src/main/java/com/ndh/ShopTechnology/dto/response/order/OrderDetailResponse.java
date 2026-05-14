package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDetailResponse {

    private Long id;
    private Long productId;
    /** Biến thể đã mua (null với đơn cũ). */
    private Long productVariantId;
    private java.util.Map<String, String> variantOptions;
    private String variantSkuCode;
    private String productName;
    private Integer quantity;
    private Double unitPrice;
    private String lineTotal;
    private String description;

    /** Mô tả dài sản phẩm tại thời điểm xem đơn (snapshot từ bảng product). */
    @JsonProperty("l_description")
    private String lDescription;
}
