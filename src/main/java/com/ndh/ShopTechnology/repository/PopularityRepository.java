package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.log.CollectorLogEntity;
import com.ndh.ShopTechnology.services.recommendation.dto.PopularProductRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Repository "đặc biệt" — chỉ chứa native aggregate query trên bảng collector_log.
 * Bind root entity = CollectorLogEntity (bảng nguồn) để Spring Data JPA dựng được metamodel.
 * Không kế thừa CrudRepository / JpaRepository nên KHÔNG mở các method CRUD ra ngoài.
 */
public interface PopularityRepository extends Repository<CollectorLogEntity, Long> {

    /**
     * Top sản phẩm phổ biến — weighted event từ collector_log:
     *   buy        × 5
     *   addToCart  × 3
     *   details    × 1
     */
    @Query(value = """
        SELECT product_id AS productId,
               SUM(CASE event
                       WHEN 'buy'       THEN 5
                       WHEN 'addToCart' THEN 3
                       WHEN 'details'   THEN 1
                       ELSE 0
                   END) AS cnt
        FROM collector_log
        WHERE product_id IS NOT NULL
        GROUP BY product_id
        HAVING cnt > 0
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<PopularProductRow> findTopPopular(Pageable pageable);

    /**
     * (Tuỳ chọn) Chỉ trending 30 ngày gần nhất — cần khi data mới nhiều.
     */
    @Query(value = """
        SELECT product_id AS productId,
               SUM(CASE event
                       WHEN 'buy'       THEN 5
                       WHEN 'addToCart' THEN 3
                       WHEN 'details'   THEN 1
                       ELSE 0
                   END) AS cnt
        FROM collector_log
        WHERE product_id IS NOT NULL
          AND `timestamp` >= (NOW() - INTERVAL 30 DAY)
        GROUP BY product_id
        HAVING cnt > 0
        ORDER BY cnt DESC
        """, nativeQuery = true)
    List<PopularProductRow> findTopTrending(Pageable pageable);
}