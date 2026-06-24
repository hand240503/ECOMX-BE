package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.UnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<UnitEntity, Long> {

    List<UnitEntity> findAllByOrderByIdAsc();

    Optional<UnitEntity> findFirstByNameUnitIgnoreCase(String nameUnit);

    boolean existsByNameUnitIgnoreCase(String nameUnit);

    boolean existsByNameUnitIgnoreCaseAndIdNot(String nameUnit, Long id);

    Optional<UnitEntity> findFirstByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
