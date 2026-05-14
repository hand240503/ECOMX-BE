package com.ndh.ShopTechnology.dto.request.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateProductVariantRequest {

    /** Mã SKU chuỗi (khuyến nghị unique). */
    private String skuCode;

    @Builder.Default
    private Map<String, String> optionValues = new LinkedHashMap<>();

    private Boolean active;

    private Integer sortOrder;

    @Valid
    private List<CreatePriceRequest> prices;
}
