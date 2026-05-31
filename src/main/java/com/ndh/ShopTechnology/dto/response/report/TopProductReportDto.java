package com.ndh.ShopTechnology.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopProductReportDto {
    private Long productId;
    private String productName;
    private String sku;
    private Long count; 
}
