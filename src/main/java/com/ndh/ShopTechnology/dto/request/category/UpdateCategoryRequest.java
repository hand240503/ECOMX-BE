package com.ndh.ShopTechnology.dto.request.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateCategoryRequest {

  private String code;
  private String name;
  private Integer status;
  private Long parentId; // Can be null to make it a root category
}
