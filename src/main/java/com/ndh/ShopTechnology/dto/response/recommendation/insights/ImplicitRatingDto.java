package com.ndh.ShopTechnology.dto.response.recommendation.insights;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Một dòng implicit rating (user_ratings type = 1) kèm tên user và tên sản phẩm.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImplicitRatingDto {

    private Long userId;
    private String userName;
    private Long productId;
    private String productName;
    private Double rating;
}
