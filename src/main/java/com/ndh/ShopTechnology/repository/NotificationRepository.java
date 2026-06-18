package com.ndh.ShopTechnology.repository;

import com.ndh.ShopTechnology.entities.notification.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByUser_IdOrderByIdDesc(Long userId, Pageable pageable);

    long countByUser_IdAndIsReadFalse(Long userId);

    Optional<NotificationEntity> findByIdAndUser_Id(Long id, Long userId);

    @Modifying
    @Query("update NotificationEntity n set n.isRead = true where n.user.id = :userId and n.isRead = false")
    int markAllReadByUser(@Param("userId") Long userId);
}
