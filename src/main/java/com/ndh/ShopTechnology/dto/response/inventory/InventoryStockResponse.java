package com.ndh.ShopTechnology.dto.response.inventory;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryStockResponse {
    private Long variantId;
    private String skuCode;
    private Long productId;
    private String productName;
    private Integer onHand;
    private Integer reserved;
    private Integer available;
}
