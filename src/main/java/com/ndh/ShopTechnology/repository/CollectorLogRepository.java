package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.log.CollectorLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface CollectorLogRepository extends JpaRepository<CollectorLogEntity, Long> {

    List<CollectorLogEntity> findByUserId(Long userId);

    List<CollectorLogEntity> findByProductId(Long productId);

    List<CollectorLogEntity> findByEvent(String event);

    List<CollectorLogEntity> findBySessionId(String sessionId);

    List<CollectorLogEntity> findByTimestampBetween(Date startDate, Date endDate);

    @Query("SELECT c FROM CollectorLogEntity c WHERE c.user.id = :userId AND c.timestamp BETWEEN :startDate AND :endDate")
    List<CollectorLogEntity> findByUserIdAndTimestampBetween(
            @Param("userId") Long userId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    @Query("SELECT c FROM CollectorLogEntity c WHERE c.product.id = :productId AND c.timestamp BETWEEN :startDate AND :endDate")
    List<CollectorLogEntity> findByProductIdAndTimestampBetween(
            @Param("productId") Long productId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );
}
