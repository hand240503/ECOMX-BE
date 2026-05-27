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

    @JsonProperty("items_subtotal")
    private Double itemsSubtotal;

    @JsonProperty("pwp_suggestions")
    private List<CheckoutPwpSuggestionDto> pwpSuggestions;
}
