package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.UnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<UnitEntity, Long> {

    List<UnitEntity> findAllByOrderByIdAsc();

    boolean existsByNameUnitIgnoreCase(String nameUnit);

    boolean existsByNameUnitIgnoreCaseAndIdNot(String nameUnit, Long id);
}
