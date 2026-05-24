package com.ndh.ShopTechnology.dto.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Nhóm sản phẩm đang chạy chương trình khuyến mãi theo loại chương trình.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivePromotionsResponse {

    /** Sản phẩm đang có Price Change (thay giá tạm thời) đang hiệu lực. */
    @JsonProperty("price_change")
    private List<ProductFullResponse> priceChange;

    /** Sản phẩm đang có Volume Price Tier (giá theo bậc số lượng) được bật. */
    @JsonProperty("volume_tier")
    private List<ProductFullResponse> volumeTier;

    /** Sản phẩm đang có Purchase-With-Purchase offer được bật (cả neo lẫn đi kèm). */
    @JsonProperty("purchase_with_purchase")
    private List<ProductFullResponse> purchaseWithPurchase;
}
