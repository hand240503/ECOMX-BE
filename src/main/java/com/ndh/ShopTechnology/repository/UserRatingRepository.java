package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.rating.UserRatingEntity;
import com.ndh.ShopTechnology.repository.projection.ProductRatingAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRatingRepository extends JpaRepository<UserRatingEntity, Long> {

    Optional<UserRatingEntity> findByUserIdAndProductId(Long userId, Long productId);

    List<UserRatingEntity> findByUserId(Long userId);

    List<UserRatingEntity> findByProductId(Long productId);

    List<UserRatingEntity> findByRating(Double rating);

    @Query("SELECT AVG(u.rating) FROM UserRatingEntity u WHERE u.product.id = :productId")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(u) FROM UserRatingEntity u WHERE u.product.id = :productId")
    Long countByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(u) FROM UserRatingEntity u WHERE u.product.id = :productId AND u.rating = :rating")
    Long countByProductIdAndRating(@Param("productId") Long productId, @Param("rating") Double rating);

    @Query("SELECT u.product.id AS productId, AVG(u.rating) AS averageRating, COUNT(u) AS ratingCount "
            + "FROM UserRatingEntity u WHERE u.product.id IN :ids GROUP BY u.product.id")
    List<ProductRatingAggregate> aggregateByProductIdIn(@Param("ids") Collection<Long> ids);
}
