package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.ProductPriceChangeUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductPriceChangeUsageRepository extends JpaRepository<ProductPriceChangeUsageEntity, Long> {

    @Query("""
            SELECT COALESCE(SUM(u.quantity), 0)
            FROM ProductPriceChangeUsageEntity u
            WHERE u.priceChangeId = :pcId
              AND u.userId = :userId
            """)
    int sumQuantityByPriceChangeIdAndUserId(
            @Param("pcId") Long pcId,
            @Param("userId") Long userId);
}
