package com.ndh.ShopTechnology.dto.response.recommendation.insights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Một cặp độ tương đồng item-item (kèm tên 2 sản phẩm) phục vụ hiển thị/báo cáo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarityPairDto {

    private Long sourceId;
    private String sourceName;
    private Long targetId;
    private String targetName;
    private BigDecimal similarity;
    private Integer rankPos;
    private String algorithm;
}
