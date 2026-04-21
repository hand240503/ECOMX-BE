package com.ndh.ShopTechnology.dto.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDetailResponse {

  private ProductFullResponse product;
  /** Similar / hybrid recommendations for PDP; never includes the current product. */
  private List<ProductFullResponse> recommendations;
}
