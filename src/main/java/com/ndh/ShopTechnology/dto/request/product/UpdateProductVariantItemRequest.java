package com.ndh.ShopTechnology.dto.request.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class UpdateProductVariantItemRequest {

    @NotNull
    private Long id;

    private String skuCode;

    private Map<String, String> optionValues;

    private Boolean active;

    private Integer sortOrder;

    @Valid
    private List<CreatePriceRequest> newPrices;

    @Valid
    private List<UpdateProductPriceItemRequest> updatedPrices;

    private List<Long> removedPriceIds;

    private List<Long> removedDocumentIds;

    private Long mainDocumentId;
}
