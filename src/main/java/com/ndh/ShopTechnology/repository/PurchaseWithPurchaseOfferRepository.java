package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.promotion.PurchaseWithPurchaseOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseWithPurchaseOfferRepository extends JpaRepository<PurchaseWithPurchaseOfferEntity, Long> {

    List<PurchaseWithPurchaseOfferEntity> findByCompanionProduct_IdInAndEnabledTrue(Collection<Long> companionIds);

    Optional<PurchaseWithPurchaseOfferEntity> findByCompanionProduct_Id(Long companionProductId);

    List<PurchaseWithPurchaseOfferEntity> findAllByOrderByIdAsc();
}
