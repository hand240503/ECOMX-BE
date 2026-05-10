package com.ndh.ShopTechnology.dto.request.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateProductRequest {

  private String productName;
  private String description;

  @JsonProperty("l_description")
  private String lDescription;

  private Integer status;
  private Long categoryId;
  private Boolean isFeatured;

  private Boolean hotSale;

  /** Partial: chỉ đổi khi gửi field. */
  private Long sku;

  /** Nhãn / tag sản phẩm (partial: chỉ đổi khi gửi field, có thể gửi chuỗi rỗng để xóa). */
  private String tag;
}
