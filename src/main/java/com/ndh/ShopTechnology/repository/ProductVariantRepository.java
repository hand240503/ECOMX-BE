package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {

    Optional<ProductVariantEntity> findFirstByProduct_IdAndActiveTrueOrderBySortOrderAscIdAsc(Long productId);

    @Query("""
            SELECT DISTINCT v FROM ProductVariant v
            JOIN FETCH v.product p
            LEFT JOIN FETCH v.prices pr
            LEFT JOIN FETCH pr.unit
            WHERE v.id IN :ids
            """)
    List<ProductVariantEntity> findAllWithProductAndPricesByIdIn(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT DISTINCT v FROM ProductVariant v
            JOIN FETCH v.product p
            LEFT JOIN FETCH v.prices pr
            LEFT JOIN FETCH pr.unit
            WHERE v.id = :id
            """)
    Optional<ProductVariantEntity> findWithProductAndPricesById(@Param("id") Long id);

    /**
     * Các biến thể active của nhiều SPU (đã sắp xếp) — dùng để chọn 1 biến thể mặc định / dòng legacy chỉ gửi {@code productId}.
     */
    @Query("""
            SELECT DISTINCT v FROM ProductVariant v
            JOIN FETCH v.product p
            WHERE p.id IN :productIds AND v.active = true
            ORDER BY p.id ASC, v.sortOrder ASC, v.id ASC
            """)
    List<ProductVariantEntity> findActiveByProductIdIn(@Param("productIds") Collection<Long> productIds);

    long countByProduct_Id(Long productId);

    boolean existsByIdAndProduct_Id(Long id, Long productId);
}
