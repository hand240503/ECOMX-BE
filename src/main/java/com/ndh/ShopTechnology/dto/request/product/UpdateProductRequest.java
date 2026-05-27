package com.ndh.ShopTechnology.dto.request.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateProductRequest {

  private String productName;
  private String description;

  @JsonProperty("l_description")
  private String lDescription;

  private Integer status;
  private Long categoryId;

  private Long brandId;

  private Boolean isFeatured;

  private Boolean hotSale;

  private Long sku;

  private String tag;

  private List<Long> removedDocumentIds;

  private Long mainDocumentId;

  private Integer mainNewImageIndex;

  @Valid
  private List<CreatePriceRequest> newPrices;

  @Valid
  private List<UpdateProductPriceItemRequest> updatedPrices;

  private List<Long> removedPriceIds;

  @Valid
  private List<CreateProductVariantRequest> newVariants;

  @Valid
  private List<UpdateProductVariantItemRequest> updatedVariants;

  private List<Long> removedVariantIds;
}
