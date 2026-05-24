package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.promotion.PurchaseWithPurchaseOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseWithPurchaseOfferRepository extends JpaRepository<PurchaseWithPurchaseOfferEntity, Long> {

    @Query("""
            SELECT DISTINCT o FROM PurchaseWithPurchaseOfferEntity o
            JOIN FETCH o.anchorProduct
            JOIN FETCH o.companionProduct
            JOIN FETCH o.anchorVariant
            JOIN FETCH o.companionVariant
            WHERE o.companionVariant.id IN :companionVariantIds AND o.enabled = true
            """)
    List<PurchaseWithPurchaseOfferEntity> findActiveFetchedByCompanionVariantIdIn(
            @Param("companionVariantIds") Collection<Long> companionVariantIds);

    @Query("""
            SELECT DISTINCT o FROM PurchaseWithPurchaseOfferEntity o
            JOIN FETCH o.anchorProduct
            JOIN FETCH o.companionProduct
            JOIN FETCH o.anchorVariant
            JOIN FETCH o.companionVariant
            WHERE o.anchorVariant.id IN :anchorVariantIds AND o.enabled = true
            """)
    List<PurchaseWithPurchaseOfferEntity> findActiveFetchedByAnchorVariantIdIn(
            @Param("anchorVariantIds") Collection<Long> anchorVariantIds);

    Optional<PurchaseWithPurchaseOfferEntity> findByCompanionVariant_Id(Long companionVariantId);

    List<PurchaseWithPurchaseOfferEntity> findAllByOrderByIdAsc();

    /** Distinct product IDs tham gia PwP (cả neo lẫn đi kèm) trong các offer đang bật. */
    @Query("""
            SELECT DISTINCT o.anchorProduct.id FROM PurchaseWithPurchaseOfferEntity o
            WHERE o.enabled = true
            """)
    List<Long> findDistinctAnchorProductIdsEnabled();

    @Query("""
            SELECT DISTINCT o.companionProduct.id FROM PurchaseWithPurchaseOfferEntity o
            WHERE o.enabled = true
            """)
    List<Long> findDistinctCompanionProductIdsEnabled();
}
