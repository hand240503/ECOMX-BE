package com.ndh.ShopTechnology.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Gợi ý mua kèm (PwP) khi SP neo đang có trong giỏ nhưng SP companion chưa được thêm.
 * Trả về trong {@link CheckoutPricingPreviewResponse#getPwpSuggestions()} để FE hiển thị
 * lựa chọn "Mua kèm giá ưu đãi" / "Không áp dụng".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutPwpSuggestionDto {

    /** Id của PwP offer. */
    @JsonProperty("offer_id")
    private Long offerId;

    /** Id SPU neo (anchor) — đã có trong lines checkout. */
    @JsonProperty("anchor_product_id")
    private Long anchorProductId;

    /** Id SKU neo. */
    @JsonProperty("anchor_variant_id")
    private Long anchorVariantId;

    /** Id SPU đi kèm (companion) — chưa có trong lines checkout. */
    @JsonProperty("companion_product_id")
    private Long companionProductId;

    /** Id SKU đi kèm. */
    @JsonProperty("companion_variant_id")
    private Long companionVariantId;

    /** Tên sản phẩm đi kèm. */
    @JsonProperty("companion_product_name")
    private String companionProductName;

    /** SKU code của variant đi kèm. */
    @JsonProperty("companion_variant_sku_code")
    private String companionVariantSkuCode;

    /** Option values của variant đi kèm (màu, dung lượng, …). */
    @JsonProperty("companion_variant_options")
    private Map<String, String> companionVariantOptions;

    /** Ảnh đại diện sản phẩm đi kèm (null nếu không có ảnh). */
    @JsonProperty("companion_thumbnail_url")
    private String companionThumbnailUrl;

    /** Giá khuyến mãi khi mua kèm với SP neo. */
    @JsonProperty("promo_unit_price")
    private Double promoUnitPrice;

    /** Giá catalog/niêm yết của SP đi kèm (để FE hiển thị so sánh). */
    @JsonProperty("companion_regular_price")
    private Double companionRegularPrice;

    /** SL neo tối thiểu để kích hoạt ưu đãi. */
    @JsonProperty("min_anchor_quantity")
    private Integer minAnchorQuantity;

    /** Số đơn vị companion được hưởng giá KM cho mỗi đơn vị anchor. */
    @JsonProperty("companion_promo_units_per_anchor")
    private Integer companionPromoUnitsPerAnchor;

    /** Giới hạn tối đa companion được hưởng giá KM (null = không giới hạn). */
    @JsonProperty("max_companion_promo_units")
    private Integer maxCompanionPromoUnits;
}
