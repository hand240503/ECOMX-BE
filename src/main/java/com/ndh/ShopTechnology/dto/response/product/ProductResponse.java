package com.ndh.ShopTechnology.dto.response.product;

import com.ndh.ShopTechnology.entities.product.ProductEntity;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse {

  private Long id;
  private String productName;
  private String description;
  private Integer status;
  private Long categoryId;
  private String categoryName;
  private String categoryCode;

  public static ProductResponse fromEntity(ProductEntity entity) {
    if (entity == null)
      return null;

    ProductResponseBuilder builder = ProductResponse.builder()
        .id(entity.getId())
        .productName(entity.getProductName())
        .description(entity.getDescription())
        .status(entity.getStatus());

    if (entity.getCategory() != null) {
      builder.categoryId(entity.getCategory().getId())
          .categoryName(entity.getCategory().getName())
          .categoryCode(entity.getCategory().getCode());
    }

    return builder.build();
  }
}
