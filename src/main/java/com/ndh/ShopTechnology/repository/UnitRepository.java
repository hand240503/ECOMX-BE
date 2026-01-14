package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.UnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitRepository extends JpaRepository<UnitEntity, Long> {
}
