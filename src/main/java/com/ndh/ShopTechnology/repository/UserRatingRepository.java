package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.rating.UserRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRatingRepository extends JpaRepository<UserRatingEntity, Long> {

    Optional<UserRatingEntity> findByUserIdAndProductId(Long userId, Long productId);

    List<UserRatingEntity> findByUserId(Long userId);

    List<UserRatingEntity> findByProductId(Long productId);

    List<UserRatingEntity> findByRating(Integer rating);

    @Query("SELECT AVG(u.rating) FROM UserRatingEntity u WHERE u.product.id = :productId")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(u) FROM UserRatingEntity u WHERE u.product.id = :productId")
    Long countByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(u) FROM UserRatingEntity u WHERE u.product.id = :productId AND u.rating = :rating")
    Long countByProductIdAndRating(@Param("productId") Long productId, @Param("rating") Integer rating);
}
