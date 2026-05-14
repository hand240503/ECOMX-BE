package com.ndh.ShopTechnology.dto.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ndh.ShopTechnology.entities.product.PriceEntity;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVariantResponse {

    private Long id;
    /** Id SPU cha — tiện FE cart / breadcrumb. */
    @JsonProperty("product_id")
    private Long productId;
    private String skuCode;
    private Map<String, String> optionValues;
    private Boolean active;
    private Integer sortOrder;
    private List<ProductPriceResponse> prices;

    @JsonProperty("created_date")
    private Date createdDate;

    @JsonProperty("modified_date")
    private Date modifiedDate;

    /**
     * Đơn giá hiển thị storefront khi có price change đang hiệu lực: {@code sale_price ?? base_price};
     * không có price change: cùng quy tắc catalog như {@link com.ndh.ShopTechnology.utils.CatalogVariantUnitPrice}
     * (dòng {@link PriceEntity} có {@code id} nhỏ nhất).
     */
    @JsonProperty("effective_unit_price")
    private Double effectiveUnitPrice;

    /** Price change đang hiệu lực tại thời điểm trả API (cùng nguồn với {@link #effectiveUnitPrice}). */
    @JsonProperty("active_price_change")
    private ProductPriceChangeResponse activePriceChange;

    /** Gallery/media của SKU ({@code document.entity_type} = PRODUCT_VARIANT). Được gán sau khi load API. */
    private List<ProductDocumentSummary> documents;
    private List<String> imageUrls;
    private String mainImageUrl;
    private String thumbnailUrl;

    @JsonProperty(value = "imageUrl", access = JsonProperty.Access.READ_ONLY)
    public String getImageUrl() {
        return mainImageUrl;
    }

    @JsonProperty(value = "coverImageUrl", access = JsonProperty.Access.READ_ONLY)
    public String getCoverImageUrl() {
        return mainImageUrl;
    }

    public static ProductVariantResponse fromEntity(ProductVariantEntity entity) {
        return fromEntity(entity, null);
    }

    public static ProductVariantResponse fromEntity(ProductVariantEntity entity, Double effectiveUnitPrice) {
        if (entity == null) {
            return null;
        }
        List<ProductPriceResponse> priceList = null;
        if (entity.getPrices() != null && !entity.getPrices().isEmpty()) {
            priceList = entity.getPrices().stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(PriceEntity::getId, Comparator.nullsLast(Long::compareTo)))
                    .map(ProductPriceResponse::fromEntity)
                    .collect(Collectors.toList());
        }
        Long productId = entity.getProduct() != null ? entity.getProduct().getId() : null;
        return ProductVariantResponse.builder()
                .id(entity.getId())
                .productId(productId)
                .skuCode(entity.getSkuCode())
                .optionValues(entity.getOptionValues())
                .active(entity.getActive())
                .sortOrder(entity.getSortOrder())
                .prices(priceList)
                .createdDate(entity.getCreatedDate())
                .modifiedDate(entity.getModifiedDate())
                .effectiveUnitPrice(effectiveUnitPrice)
                .build();
    }
}
