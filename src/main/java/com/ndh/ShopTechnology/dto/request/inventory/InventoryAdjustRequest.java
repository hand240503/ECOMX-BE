package com.ndh.ShopTechnology.dto.request.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryAdjustRequest {

    @NotNull(message = "variantId là bắt buộc")
    private Long variantId;

    @NotNull(message = "onHand là bắt buộc")
    @Min(value = 0, message = "Tồn kho không được âm")
    private Integer onHand;

    private String note;
}
