package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findByCategoryId(Long categoryId);

    @Query("SELECT p FROM products p WHERE p.category.id = :categoryId")
    List<ProductEntity> findByCategoryIdWithCategory(@Param("categoryId") Long categoryId);

    Optional<ProductEntity> findByProductName(String productName);

    List<ProductEntity> findByStatus(Integer status);

    // Get Featured Products
    List<ProductEntity> findByIsFeaturedTrue(Pageable pageable);

    // Get Best Sellers (Top N by sold_count)
    List<ProductEntity> findTopNByOrderBySoldCountDesc(Pageable pageable);
}
