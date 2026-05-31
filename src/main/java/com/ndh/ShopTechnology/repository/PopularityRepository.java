package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.log.CollectorLogEntity;
import com.ndh.ShopTechnology.services.recommendation.dto.PopularProductRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface PopularityRepository extends Repository<CollectorLogEntity, Long> {

    /** Toàn thời gian — bao gồm cả sản phẩm chưa có tương tác (cnt = 0) */
    @Query(value = """
        SELECT p.id AS productId,
               COALESCE(SUM(CASE cl.event
                                WHEN 'buy'         THEN 5
                                WHEN 'moreDetails' THEN 2
                                WHEN 'details'     THEN 1
                                ELSE 0
                            END), 0) AS cnt
        FROM products p
        LEFT JOIN collector_log cl
               ON cl.product_id = p.id
              AND cl.event IN ('details', 'moreDetails', 'buy')
        GROUP BY p.id
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<PopularProductRow> findTopPopular(Pageable pageable);

    /** Theo khoảng thời gian — bao gồm cả sản phẩm chưa có tương tác trong kỳ */
    @Query(value = """
        SELECT p.id AS productId,
               COALESCE(SUM(CASE cl.event
                                WHEN 'buy'         THEN 5
                                WHEN 'moreDetails' THEN 2
                                WHEN 'details'     THEN 1
                                ELSE 0
                            END), 0) AS cnt
        FROM products p
        LEFT JOIN collector_log cl
               ON cl.product_id = p.id
              AND cl.event IN ('details', 'moreDetails', 'buy')
              AND cl.`timestamp` >= :since
        GROUP BY p.id
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<PopularProductRow> findTopTrendingSince(
            @org.springframework.data.repository.query.Param("since") java.util.Date since,
            Pageable pageable);
}
