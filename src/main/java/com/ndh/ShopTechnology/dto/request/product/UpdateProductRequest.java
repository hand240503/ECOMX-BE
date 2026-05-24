package com.ndh.ShopTechnology.dto.request.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

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

  /** Partial: chỉ đổi khi gửi field. */
  private Long brandId;

  private Boolean isFeatured;

  private Boolean hotSale;

  /** Partial: chỉ đổi khi gửi field. */
  private Long sku;

  /** Nhãn / tag sản phẩm (partial: chỉ đổi khi gửi field, có thể gửi chuỗi rỗng để xóa). */
  private String tag;

  /**
   * ID các bản ghi {@code document} (ảnh/media sản phẩm) cần xóa trong cùng request cập nhật.
   * Chỉ áp dụng cho document thuộc đúng sản phẩm ({@code entity_type} sản phẩm).
   */
  private List<Long> removedDocumentIds;

  /**
   * Sau khi xóa/thêm ảnh: đặt document này làm ảnh đại diện (main). Phải thuộc đúng sản phẩm đang sửa.
   */
  private Long mainDocumentId;

  /**
   * Chỉ dùng kèm upload file mới (multipart {@code newImages}): chỉ số 0-based trong danh sách file mới
   * được đánh dấu main (ưu tiên thấp hơn {@link #mainDocumentId} nếu cả hai được gửi).
   */
  private Integer mainNewImageIndex;

  /**
   * Thêm entry giá catalog mới (bảng {@code price}) — cùng schema {@link CreatePriceRequest}.
   */
  @Valid
  private List<CreatePriceRequest> newPrices;

  /**
   * Cập nhật các entry giá đã tồn tại (phải thuộc đúng sản phẩm đang sửa).
   */
  @Valid
  private List<UpdateProductPriceItemRequest> updatedPrices;

  /** ID các bản ghi {@code price} cần xóa khỏi sản phẩm này. */
  private List<Long> removedPriceIds;

  /**
   * Thêm biến thể mới (kèm giá tùy chọn).
   */
  @Valid
  private List<CreateProductVariantRequest> newVariants;

  @Valid
  private List<UpdateProductVariantItemRequest> updatedVariants;

  private List<Long> removedVariantIds;
}
