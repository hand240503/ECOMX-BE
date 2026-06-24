package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<BrandEntity, Long> {

    List<BrandEntity> findAllByOrderByIdAsc();

    Optional<BrandEntity> findFirstByCodeIgnoreCase(String code);

    Optional<BrandEntity> findFirstByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    @Query("""
            SELECT DISTINCT p.brand FROM products p
            WHERE p.brand IS NOT NULL
            AND (p.category.id = :categoryId OR p.category.parent.id = :categoryId)
            ORDER BY p.brand.name ASC
            """)
    List<BrandEntity> findDistinctBrandsByCategoryId(@Param("categoryId") Long categoryId);
}
