package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.recommendation.ItemSimilarityEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemSimilarityRepository
        extends JpaRepository<ItemSimilarityEntity, Long> {

    @Query("""
        SELECT s FROM ItemSimilarityEntity s
        WHERE s.source = :source AND s.algorithm = :algorithm
        ORDER BY s.rankPos ASC
    """)
    List<ItemSimilarityEntity> findTopBySource(
            @Param("source") Integer source,
            @Param("algorithm") String algorithm,
            Pageable pageable);

    @Query("""
        SELECT s FROM ItemSimilarityEntity s
        WHERE s.source IN :sources
          AND s.algorithm = :algorithm
          AND s.target NOT IN :excluded
        ORDER BY s.similarity DESC
    """)
    List<ItemSimilarityEntity> findBySourcesExcluding(
            @Param("sources") List<Integer> sources,
            @Param("algorithm") String algorithm,
            @Param("excluded") List<Integer> excluded,
            Pageable pageable);
}