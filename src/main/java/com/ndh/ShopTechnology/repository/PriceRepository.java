package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.product.PriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PriceRepository extends JpaRepository<PriceEntity, Long> {

    @Query("SELECT pr FROM price pr LEFT JOIN FETCH pr.unit INNER JOIN pr.variant v "
            + "WHERE v.id IN :variantIds ORDER BY v.id ASC, pr.id ASC")
    List<PriceEntity> findAllWithUnitByVariantIdIn(@Param("variantIds") Collection<Long> variantIds);

    List<PriceEntity> findByVariant_IdOrderByIdAsc(Long variantId);

    /** Tất cả entry giá của mọi biến thể thuộc một SPU (SPU id = {@code products.id}). */
    List<PriceEntity> findByVariant_Product_IdOrderByVariant_IdAscIdAsc(Long productId);

    /**
     * Giống {@link #findByVariant_Product_IdOrderByVariant_IdAscIdAsc} nhưng fetch variant + product + unit một query
     * (admin list giá kèm ô tóm tắt SKU).
     */
    @Query(
            """
                    SELECT pr FROM price pr
                    LEFT JOIN FETCH pr.unit
                    LEFT JOIN FETCH pr.variant v
                    LEFT JOIN FETCH v.product p
                    WHERE p.id = :productId
                    ORDER BY v.id ASC, pr.id ASC
                    """)
    List<PriceEntity> findAllWithVariantAndUnitByProductIdOrderByVariantIdAscIdAsc(@Param("productId") Long productId);

    long countByUnit_Id(Long unitId);
}
