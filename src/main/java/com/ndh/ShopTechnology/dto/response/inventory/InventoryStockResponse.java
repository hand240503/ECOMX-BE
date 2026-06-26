package com.ndh.ShopTechnology.dto.response.inventory;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryStockResponse {
    private Long storeId;
    private String storeName;
    private Long variantId;
    private String skuCode;
    private Long productId;
    private String productName;
    private Map<String, String> optionValues;
    private Integer onHand;
    private Integer reserved;
    private Integer available;
}
