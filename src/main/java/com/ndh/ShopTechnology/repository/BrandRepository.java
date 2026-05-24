package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.BrandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<BrandEntity, Long> {

    List<BrandEntity> findAllByOrderByIdAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    /**
     * Lấy các brand phân biệt có sản phẩm thuộc danh mục {@code categoryId}
     * hoặc danh mục con trực tiếp của nó.
     */
    @Query("""
            SELECT DISTINCT p.brand FROM products p
            WHERE p.brand IS NOT NULL
            AND (p.category.id = :categoryId OR p.category.parent.id = :categoryId)
            ORDER BY p.brand.name ASC
            """)
    List<BrandEntity> findDistinctBrandsByCategoryId(@Param("categoryId") Long categoryId);
}
