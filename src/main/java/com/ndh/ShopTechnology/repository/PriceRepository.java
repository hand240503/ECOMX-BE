package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.PriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceRepository extends JpaRepository<PriceEntity, Long> {
}
