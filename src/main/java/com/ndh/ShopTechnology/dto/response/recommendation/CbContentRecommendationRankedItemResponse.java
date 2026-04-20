package com.ndh.ShopTechnology.dto.response.recommendation;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CbContentRecommendationRankedItemResponse {

    private int rankPos;
    private Long productId;
    private Double similarity;
}
