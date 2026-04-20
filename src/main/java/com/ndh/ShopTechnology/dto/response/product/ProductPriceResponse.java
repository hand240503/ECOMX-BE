package com.ndh.ShopTechnology.dto.response.product;

import com.ndh.ShopTechnology.entities.product.PriceEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceResponse {

  private Long id;
  private Double currentValue;
  private Double oldValue;
  private Long unitId;
  private String unitName;
  private Integer unitRatio;

  public static ProductPriceResponse fromEntity(PriceEntity entity) {
    if (entity == null) {
      return null;
    }
    ProductPriceResponseBuilder b = ProductPriceResponse.builder()
        .id(entity.getId())
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
