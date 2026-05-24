package com.ndh.ShopTechnology.dto.response.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ndh.ShopTechnology.dto.response.product.BrandSummaryResponse;
import com.ndh.ShopTechnology.entities.product.CategoryEntity;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {

  private Long id;
  private String code;
  private String name;
  private Integer status;
  private Long parentId;
  private String parentName;
  private List<CategoryResponse> children;
  private Integer childrenCount;
  /** URL ảnh đại diện (is_main = true) của danh mục; {@code null} nếu chưa upload. */
  private String thumbnailUrl;
  /** Danh sách brand có sản phẩm thuộc danh mục này (chỉ trả về ở /roots). */
  private List<BrandSummaryResponse> brands;

  public static CategoryResponse fromEntity(CategoryEntity entity) {
    return fromEntity(entity, null);
  }

  public static CategoryResponse fromEntity(CategoryEntity entity, String thumbnailUrl) {
    if (entity == null)
      return null;

    CategoryResponseBuilder builder = CategoryResponse.builder()
        .id(entity.getId())
        .code(entity.getCode())
        .name(entity.getName())
        .status(entity.getStatus())
        .thumbnailUrl(thumbnailUrl);

    if (entity.getParent() != null) {
      builder.parentId(entity.getParent().getId())
          .parentName(entity.getParent().getName());
    }

    if (entity.getChildren() != null) {
      builder.childrenCount(entity.getChildren().size())
          .children(entity.getChildren().stream()
              .map(c -> CategoryResponse.fromEntity(c, null))
              .collect(Collectors.toList()));
    } else {
      builder.childrenCount(0);
    }

    return builder.build();
  }

  public static CategoryResponse fromEntitySimple(CategoryEntity entity) {
    return fromEntitySimple(entity, null);
  }

  public static CategoryResponse fromEntitySimple(CategoryEntity entity, String thumbnailUrl) {
    if (entity == null)
      return null;

    CategoryResponseBuilder builder = CategoryResponse.builder()
        .id(entity.getId())
        .code(entity.getCode())
        .name(entity.getName())
        .status(entity.getStatus())
        .thumbnailUrl(thumbnailUrl);

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
