package com.ndh.ShopTechnology.dto.response.product;

import com.ndh.ShopTechnology.entities.product.BrandEntity;
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
public class BrandSummaryResponse {

  private Long id;
  private String code;
  private String name;
  private Integer status;

  public static BrandSummaryResponse fromEntity(BrandEntity entity) {
    if (entity == null) {
      return null;
    }
    return BrandSummaryResponse.builder()
        .id(entity.getId())
        .code(entity.getCode())
        .name(entity.getName())
        .status(entity.getStatus())
        .build();
  }
}
