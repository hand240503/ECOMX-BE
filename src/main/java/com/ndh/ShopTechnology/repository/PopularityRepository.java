package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.log.CollectorLogEntity;
import com.ndh.ShopTechnology.services.recommendation.dto.PopularProductRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface PopularityRepository extends Repository<CollectorLogEntity, Long> {

    @Query(value = """
        SELECT product_id AS productId,
               SUM(CASE event
                       WHEN 'buy'          THEN 5
                       WHEN 'moreDetails' THEN 2
                       WHEN 'details'     THEN 1
                       ELSE 0
                   END) AS cnt
        FROM collector_log
        WHERE product_id IS NOT NULL
          AND event IN ('details', 'moreDetails', 'buy')
        GROUP BY product_id
        HAVING cnt > 0
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<PopularProductRow> findTopPopular(Pageable pageable);

    @Query(value = """
        SELECT product_id AS productId,
               SUM(CASE event
                       WHEN 'buy'          THEN 5
                       WHEN 'moreDetails' THEN 2
                       WHEN 'details'     THEN 1
                       ELSE 0
                   END) AS cnt
        FROM collector_log
        WHERE product_id IS NOT NULL
          AND event IN ('details', 'moreDetails', 'buy')
          AND `timestamp` >= (NOW() - INTERVAL 30 DAY)
        GROUP BY product_id
        HAVING cnt > 0
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<PopularProductRow> findTopTrending(Pageable pageable);
}
