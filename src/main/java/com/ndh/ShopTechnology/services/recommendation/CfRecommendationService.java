package com.ndh.ShopTechnology.services.recommendation;

import com.ndh.ShopTechnology.dto.response.recommendation.RecommendationItem;

import java.util.Collection;
import java.util.List;

public interface CfRecommendationService {

    /**
     * Lấy top-N item tương tự 1 product theo CF (item-item cosine).
     *
     * @param productId  product nguồn
     * @param limit      số kết quả mong muốn (sẽ bị clamp 1..MAX_LIMIT)
     * @param excludeIds product cần loại (vd. user đã mua / trong session);
     *                   nullable
     */
    List<RecommendationItem> getSimilar(
            Integer productId, int limit, Collection<Integer> excludeIds);

}