package com.ndh.ShopTechnology.dto.response.category;

import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryResponse {

  private Long id;
  private String code;
  private String name;
  private Integer status;
  private Long parentId;
  private String parentName;
  private List<CategoryResponse> children;
  private Integer childrenCount;

  public static CategoryResponse fromEntity(CategoryEntity entity) {
    if (entity == null)
      return null;

    CategoryResponseBuilder builder = CategoryResponse.builder()
        .id(entity.getId())
        .code(entity.getCode())
        .name(entity.getName())
        .status(entity.getStatus());

    if (entity.getParent() != null) {
      builder.parentId(entity.getParent().getId())
          .parentName(entity.getParent().getName());
    }

    if (entity.getChildren() != null) {
      builder.childrenCount(entity.getChildren().size())
          .children(entity.getChildren().stream()
              .map(CategoryResponse::fromEntity)
              .collect(Collectors.toList()));
    } else {
      builder.childrenCount(0);
    }

    return builder.build();
  }

  public static CategoryResponse fromEntitySimple(CategoryEntity entity) {
    if (entity == null)
      return null;

    CategoryResponseBuilder builder = CategoryResponse.builder()
        .id(entity.getId())
        .code(entity.getCode())
        .name(entity.getName())
        .status(entity.getStatus());

    if (entity.getParent() != null) {
      builder.parentId(entity.getParent().getId())
          .parentName(entity.getParent().getName());
    }

    if (entity.getChildren() != null) {
      builder.childrenCount(entity.getChildren().size());
    } else {
      builder.childrenCount(0);
    }

    return builder.build();
  }
}
