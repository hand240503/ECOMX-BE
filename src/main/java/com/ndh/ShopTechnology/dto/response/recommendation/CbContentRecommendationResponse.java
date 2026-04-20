package com.ndh.ShopTechnology.dto.response.recommendation;

import com.ndh.ShopTechnology.entities.recommendation.CbContentRecommendation;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CbContentRecommendationResponse {

    private Long id;
    private Long userId;
    private Integer topK;
    private Boolean excludeSeen;
    private LocalDateTime computedAt;
    private List<CbContentRecommendationRankedItemResponse> items;

    public static CbContentRecommendationResponse fromEntity(CbContentRecommendation entity, int maxItems) {
        if (entity == null) {
            return null;
        }
        List<CbContentRecommendationRankedItemResponse> ranked = new ArrayList<>();
        List<Long> pids = entity.getProductIds();
        List<Double> sims = entity.getSimilarities();
        if (pids != null && sims != null) {
            int n = Math.min(pids.size(), sims.size());
            int cap = maxItems > 0 ? Math.min(n, maxItems) : n;
            for (int i = 0; i < cap; i++) {
                ranked.add(CbContentRecommendationRankedItemResponse.builder()
                        .rankPos(i + 1)
                        .productId(pids.get(i))
                        .similarity(sims.get(i))
                        .build());
            }
        }
        return CbContentRecommendationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .topK(entity.getTopK())
                .excludeSeen(entity.getExcludeSeen())
                .computedAt(entity.getComputedAt())
                .items(ranked)
                .build();
    }
}
