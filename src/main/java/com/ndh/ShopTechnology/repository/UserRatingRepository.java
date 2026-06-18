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

    // ── Hiển thị/đếm: CHỈ explicit (type = 0 hoặc NULL), thang sao 1–5 ──
    // (loại implicit type=1 thang 0–10 do recommend builder sinh)

    /** Chỉ đánh giá explicit của 1 user (loại implicit do builder sinh). */
    @Query("SELECT u FROM UserRatingEntity u WHERE u.user.id = :userId AND (u.type = 0 OR u.type IS NULL)")
    List<UserRatingEntity> findExplicitByUserId(@Param("userId") Long userId);

    /** Chỉ đánh giá explicit của 1 sản phẩm. */
    @Query("SELECT u FROM UserRatingEntity u WHERE u.product.id = :productId AND (u.type = 0 OR u.type IS NULL)")
    List<UserRatingEntity> findExplicitByProductId(@Param("productId") Long productId);

    @Query("SELECT AVG(u.rating) FROM UserRatingEntity u WHERE u.product.id = :productId AND (u.type = 0 OR u.type IS NULL)")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(u) FROM UserRatingEntity u WHERE u.product.id = :productId AND (u.type = 0 OR u.type IS NULL)")
    Long countByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(u) FROM UserRatingEntity u WHERE u.product.id = :productId AND u.rating = :rating AND (u.type = 0 OR u.type IS NULL)")
    Long countByProductIdAndRating(@Param("productId") Long productId, @Param("rating") Double rating);

    @Query("SELECT u.product.id AS productId, AVG(u.rating) AS averageRating, COUNT(u) AS ratingCount "
            + "FROM UserRatingEntity u WHERE u.product.id IN :ids AND (u.type = 0 OR u.type IS NULL) GROUP BY u.product.id")
    List<ProductRatingAggregate> aggregateByProductIdIn(@Param("ids") Collection<Long> ids);
}
