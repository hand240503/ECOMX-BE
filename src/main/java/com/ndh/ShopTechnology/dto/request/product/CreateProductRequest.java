package com.ndh.ShopTechnology.dto.request.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateProductRequest {

  @NotBlank(message = "Product name is required")
  private String productName;

  private String description;

  private Integer status;

  @NotNull(message = "Category ID is required")
  private Long categoryId;

  private java.util.List<CreatePriceRequest> prices;

  private Boolean isFeatured;
}
