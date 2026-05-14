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

    List<ProductVolumePriceTierEntity> findByProduct_IdInAndEnabledTrue(Collection<Long> productIds);

    @Query("""
            SELECT t FROM ProductVolumePriceTierEntity t
            JOIN FETCH t.product p
            WHERE p.id IN :productIds AND t.enabled = true
            """)
    List<ProductVolumePriceTierEntity> findActiveFetchedByProductIdIn(@Param("productIds") Collection<Long> productIds);

    List<ProductVolumePriceTierEntity> findByProduct_IdOrderByMinQuantityAsc(Long productId);
}
