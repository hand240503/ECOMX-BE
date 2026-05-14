package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPriceChangeRepository extends JpaRepository<ProductPriceChangeEntity, Long> {

    List<ProductPriceChangeEntity> findByProductVariant_IdOrderByStartAtDesc(Long variantId);

    @Query("""
            SELECT pc FROM ProductPriceChangeEntity pc
            WHERE pc.enabled = true
              AND pc.productVariant.id = :variantId
              AND pc.startAt <= :at
              AND (pc.endAt IS NULL OR pc.endAt >= :at)
            ORDER BY pc.startAt DESC, pc.id DESC
            """)
    Optional<ProductPriceChangeEntity> findEffectiveForVariantAt(
            @Param("variantId") Long variantId,
            @Param("at") Date at);

    /**
     * Mọi dòng price change có thể đang hiệu lực tại {@code at} cho các biến thể trong {@code variantIds}.
     * Đã sắp xếp để lấy “dòng ưu tiên” theo cùng quy tắc {@link #findEffectiveForVariantAt}: startAt DESC, id DESC.
     */
    @Query("""
            SELECT pc FROM ProductPriceChangeEntity pc
            WHERE pc.enabled = true
              AND pc.productVariant.id IN :variantIds
              AND pc.startAt <= :at
              AND (pc.endAt IS NULL OR pc.endAt >= :at)
            ORDER BY pc.productVariant.id ASC, pc.startAt DESC, pc.id DESC
            """)
    List<ProductPriceChangeEntity> findAllActiveCandidatesForVariantIdsAt(
            @Param("variantIds") Collection<Long> variantIds,
            @Param("at") Date at);
}

