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

    List<PurchaseWithPurchaseOfferEntity> findByCompanionProduct_IdInAndEnabledTrue(Collection<Long> companionIds);

    @Query("""
            SELECT DISTINCT o FROM PurchaseWithPurchaseOfferEntity o
            JOIN FETCH o.anchorProduct
            JOIN FETCH o.companionProduct
            WHERE o.companionProduct.id IN :companionIds AND o.enabled = true
            """)
    List<PurchaseWithPurchaseOfferEntity> findActiveFetchedByCompanionProductIdIn(
            @Param("companionIds") Collection<Long> companionIds);

    @Query("""
            SELECT DISTINCT o FROM PurchaseWithPurchaseOfferEntity o
            JOIN FETCH o.anchorProduct
            JOIN FETCH o.companionProduct
            WHERE o.anchorProduct.id IN :anchorIds AND o.enabled = true
            """)
    List<PurchaseWithPurchaseOfferEntity> findActiveFetchedByAnchorProductIdIn(
            @Param("anchorIds") Collection<Long> anchorIds);

    Optional<PurchaseWithPurchaseOfferEntity> findByCompanionProduct_Id(Long companionProductId);

    List<PurchaseWithPurchaseOfferEntity> findAllByOrderByIdAsc();
}
