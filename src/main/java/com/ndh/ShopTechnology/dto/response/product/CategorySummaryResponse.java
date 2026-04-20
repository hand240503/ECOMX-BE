package com.ndh.ShopTechnology.dto.response.product;

import com.ndh.ShopTechnology.entities.product.CategoryEntity;
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
public class CategorySummaryResponse {

  private Long id;
  private String code;
  private String name;
  private Integer status;
  private Long parentId;

  public static CategorySummaryResponse fromEntity(CategoryEntity entity) {
    if (entity == null) {
      return null;
    }
    Long parentId = null;
    if (entity.getParent() != null) {
      parentId = entity.getParent().getId();
    }
    return CategorySummaryResponse.builder()
        .id(entity.getId())
        .code(entity.getCode())
        .name(entity.getName())
        .status(entity.getStatus())
        .parentId(parentId)
        .build();
  }
}
