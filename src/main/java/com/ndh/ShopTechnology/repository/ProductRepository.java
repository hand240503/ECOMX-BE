package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findByCategoryId(Long categoryId);

    @Query("SELECT p FROM products p WHERE p.category.id = :categoryId")
    List<ProductEntity> findByCategoryIdWithCategory(@Param("categoryId") Long categoryId);

    Optional<ProductEntity> findByProductName(String productName);

    List<ProductEntity> findByStatus(String status);
}
