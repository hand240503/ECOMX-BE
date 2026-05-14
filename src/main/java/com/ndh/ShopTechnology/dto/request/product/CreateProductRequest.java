package com.ndh.ShopTechnology.dto.request.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateProductRequest {

  @NotBlank(message = "Product name is required")
  private String productName;

  private String description;

  @JsonProperty("l_description")
  private String lDescription;

  private Integer status;

  @NotNull(message = "Category ID is required")
  private Long categoryId;

  /** Mã SKU (số), tùy chọn. */
  private Long sku;

  /**
   * Biến thể (dung lượng, màu, …) kèm giá. Khi rỗng và {@link #prices} có dữ liệu,
   * hệ thống tạo một biến thể mặc định và gắn {@link #prices} vào đó.
   */
  @Valid
  private List<CreateProductVariantRequest> variants;

  @Valid
  private List<CreatePriceRequest> prices;

  private Boolean isFeatured;

  private Boolean hotSale;
}
