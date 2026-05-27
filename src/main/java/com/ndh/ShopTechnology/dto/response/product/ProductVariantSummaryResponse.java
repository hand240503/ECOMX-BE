package com.ndh.ShopTechnology.dto.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductVariantSummaryResponse {

    private Long id;

    @JsonProperty("product_id")
    private Long productId;

    private String skuCode;

    private Map<String, String> optionValues;

    private Boolean active;

    private Integer sortOrder;

    public static ProductVariantSummaryResponse fromEntity(ProductVariantEntity entity) {
        if (entity == null) {
            return null;
        }
        Map<String, String> opts = entity.getOptionValues();
        return ProductVariantSummaryResponse.builder()
                .id(entity.getId())
                .productId(entity.getProduct() != null ? entity.getProduct().getId() : null)
                .skuCode(entity.getSkuCode())
                .optionValues(opts != null ? new LinkedHashMap<>(opts) : null)
                .active(entity.getActive())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}
