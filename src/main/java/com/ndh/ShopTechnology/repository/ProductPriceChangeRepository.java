package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.ProductPriceChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Tìm chương trình đổi giá theo KHÓA NGHIỆP VỤ (biến thể + khung thời gian) để upsert khi import.
     * Xử lý cả trường hợp endAt = null (chương trình vô thời hạn).
     */
    @Query("""
            SELECT pc FROM ProductPriceChangeEntity pc
            WHERE pc.productVariant.id = :variantId
              AND pc.startAt = :startAt
              AND ((:endAt IS NULL AND pc.endAt IS NULL) OR pc.endAt = :endAt)
            ORDER BY pc.id ASC
            """)
    List<ProductPriceChangeEntity> findByVariantAndWindow(
            @Param("variantId") Long variantId,
            @Param("startAt") Date startAt,
            @Param("endAt") Date endAt);

    @Query("""
            SELECT pc FROM ProductPriceChangeEntity pc
            WHERE pc.enabled = true
              AND pc.productVariant.id = :variantId
              AND pc.startAt <= :at
              AND (pc.endAt IS NULL OR pc.endAt >= :at)
              AND (pc.quantityLimit IS NULL OR pc.soldQuantity < pc.quantityLimit)
            ORDER BY pc.startAt DESC, pc.id DESC
            """)
    Optional<ProductPriceChangeEntity> findEffectiveForVariantAt(
            @Param("variantId") Long variantId,
            @Param("at") Date at);

    @Query("""
            SELECT pc FROM ProductPriceChangeEntity pc
            WHERE pc.enabled = true
              AND pc.productVariant.id IN :variantIds
              AND pc.startAt <= :at
              AND (pc.endAt IS NULL OR pc.endAt >= :at)
              AND (pc.quantityLimit IS NULL OR pc.soldQuantity < pc.quantityLimit)
            ORDER BY pc.productVariant.id ASC, pc.startAt DESC, pc.id DESC
            """)
    List<ProductPriceChangeEntity> findAllActiveCandidatesForVariantIdsAt(
            @Param("variantIds") Collection<Long> variantIds,
            @Param("at") Date at);

    @Query("""
            SELECT DISTINCT pc.productId FROM ProductPriceChangeEntity pc
            WHERE pc.enabled = true
              AND pc.startAt <= :at
              AND (pc.endAt IS NULL OR pc.endAt >= :at)
              AND (pc.quantityLimit IS NULL OR pc.soldQuantity < pc.quantityLimit)
            """)
    List<Long> findDistinctProductIdsWithActivePCAt(@Param("at") Date at);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ProductPriceChangeEntity pc
            SET pc.soldQuantity = pc.soldQuantity + :qty
            WHERE pc.id = :pcId
              AND (pc.quantityLimit IS NULL OR pc.soldQuantity + :qty <= pc.quantityLimit)
            """)
    int incrementSoldQuantity(@Param("pcId") Long pcId, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ProductPriceChangeEntity pc
            SET pc.soldQuantity = GREATEST(0, pc.soldQuantity - :qty)
            WHERE pc.id = :pcId
            """)
    int decrementSoldQuantity(@Param("pcId") Long pcId, @Param("qty") int qty);
}
