package com.ndh.ShopTechnology.dto.request.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryImportRequest {

    @NotNull(message = "storeId là bắt buộc")
    private Long storeId;

    @NotNull(message = "variantId là bắt buộc")
    private Long variantId;

    @NotNull(message = "quantity là bắt buộc")
    @Min(value = 1, message = "Số lượng nhập phải >= 1")
    private Integer quantity;

    private String note;
}
