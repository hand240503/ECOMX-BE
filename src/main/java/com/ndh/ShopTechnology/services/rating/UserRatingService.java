package com.ndh.ShopTechnology.services.rating;

import com.ndh.ShopTechnology.dto.request.rating.CreateUserRatingRequest;
import com.ndh.ShopTechnology.dto.request.rating.UpdateUserRatingRequest;
import com.ndh.ShopTechnology.dto.response.rating.UserRatingResponse;

import java.util.List;

public interface UserRatingService {
    UserRatingResponse createRating(CreateUserRatingRequest request);
    UserRatingResponse getRatingById(Long id);
    UserRatingResponse getRatingByUserAndProduct(Long userId, Long productId);
    List<UserRatingResponse> getRatingsByUserId(Long userId);
    List<UserRatingResponse> getRatingsByProductId(Long productId);
    List<UserRatingResponse> getAllRatings();
    UserRatingResponse updateRating(Long id, UpdateUserRatingRequest request);
    void deleteRating(Long id);
    Double getAverageRatingByProductId(Long productId);
    Long getRatingCountByProductId(Long productId);
}
