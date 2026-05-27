package com.ndh.ShopTechnology.dto.response.product;

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
public class ActivePromotionsResponse {

    @JsonProperty("price_change")
    private List<ProductFullResponse> priceChange;

    @JsonProperty("volume_tier")
    private List<ProductFullResponse> volumeTier;

    @JsonProperty("purchase_with_purchase")
    private List<ProductFullResponse> purchaseWithPurchase;
}
