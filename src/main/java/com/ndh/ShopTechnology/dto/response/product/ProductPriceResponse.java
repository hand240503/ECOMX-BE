package com.ndh.ShopTechnology.dto.response.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ndh.ShopTechnology.entities.product.PriceEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceResponse {

  private Long id;

  @JsonProperty("product_variant_id")
  private Long productVariantId;

  private Double currentValue;
  private Double oldValue;
  private Long unitId;
  private String unitName;
  private Integer unitRatio;

  /** Thông tin SKU (không kèm giá) — tiện admin list giá theo SPU. */
  private ProductVariantSummaryResponse variant;

  public static ProductPriceResponse fromEntity(PriceEntity entity) {
    if (entity == null) {
      return null;
    }
    ProductVariantSummaryResponse variantSummary = null;
    if (entity.getVariant() != null) {
      Hibernate.initialize(entity.getVariant());
      variantSummary = ProductVariantSummaryResponse.fromEntity(entity.getVariant());
    }
    ProductPriceResponseBuilder b = ProductPriceResponse.builder()
        .id(entity.getId())
        .productVariantId(entity.getVariant() != null ? entity.getVariant().getId() : null)
        .variant(variantSummary)
        .currentValue(entity.getCurrentValue())
        .oldValue(entity.getOldValue());
    if (entity.getUnit() != null) {
      b.unitId(entity.getUnit().getId())
          .unitName(entity.getUnit().getNameUnit())
          .unitRatio(entity.getUnit().getRatio());
    }
    return b.build();
  }
}
