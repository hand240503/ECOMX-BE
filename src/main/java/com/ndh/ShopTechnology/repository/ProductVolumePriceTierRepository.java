package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.promotion.ProductVolumePriceTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductVolumePriceTierRepository extends JpaRepository<ProductVolumePriceTierEntity, Long> {

    List<ProductVolumePriceTierEntity> findByProductVariant_IdInAndEnabledTrue(Collection<Long> variantIds);

    @Query("""
            SELECT DISTINCT t FROM ProductVolumePriceTierEntity t
            JOIN FETCH t.productVariant pv
            JOIN FETCH pv.product p
            WHERE pv.id IN :variantIds AND t.enabled = true
            """)
    List<ProductVolumePriceTierEntity> findActiveFetchedByProductVariantIdIn(@Param("variantIds") Collection<Long> variantIds);

    List<ProductVolumePriceTierEntity> findByProductVariant_IdIn(Collection<Long> variantIds);

    List<ProductVolumePriceTierEntity> findByProductVariant_IdOrderByMinQuantityAsc(Long variantId);

    /** Distinct product IDs có ít nhất một volume tier được bật. */
    @Query("""
            SELECT DISTINCT t.productVariant.product.id FROM ProductVolumePriceTierEntity t
            WHERE t.enabled = true
            """)
    List<Long> findDistinctProductIdsWithEnabledTiers();
}
