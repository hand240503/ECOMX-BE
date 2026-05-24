package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutPricingPreviewResponse {

    private List<CheckoutPricingLineItemResponse> lines;

    /** Tổng tiền hàng (chưa ship, chưa thuế). */
    @JsonProperty("items_subtotal")
    private Double itemsSubtotal;

    /**
     * Danh sách gợi ý mua kèm PwP: SP neo đang có trong giỏ nhưng SP companion chưa được thêm vào.
     * FE dùng để hiển thị lựa chọn "Mua kèm giá ưu đãi" / "Không áp dụng" bên dưới mỗi dòng SP neo.
     * Empty list khi không có gợi ý nào.
     */
    @JsonProperty("pwp_suggestions")
    private List<CheckoutPwpSuggestionDto> pwpSuggestions;
}
