package com.ndh.ShopTechnology.dto.response.inventory;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryLedgerResponse {
    private Long id;
    private Long variantId;
    private String movementType;
    private Integer quantity;
    private Integer sumBegin;
    private Integer sumEnd;
    private Long orderDetailId;
    private String note;
    private Date createdDate;
}
