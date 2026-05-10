package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPriceChangeRepository extends JpaRepository<ProductPriceChangeEntity, Long> {

    List<ProductPriceChangeEntity> findByProduct_IdOrderByStartAtDesc(Long productId);

    @Query("""
            SELECT pc FROM ProductPriceChangeEntity pc
            WHERE pc.enabled = true
              AND pc.product.id = :productId
              AND pc.startAt <= :at
              AND (pc.endAt IS NULL OR pc.endAt >= :at)
            ORDER BY pc.startAt DESC, pc.id DESC
            """)
    Optional<ProductPriceChangeEntity> findEffectiveForProductAt(
            @Param("productId") Long productId,
            @Param("at") Date at);
}

