package com.ndh.ShopTechnology.repository.task;

import com.ndh.ShopTechnology.entities.task.TaskNotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface TaskNotificationRepository extends JpaRepository<TaskNotificationEntity, Long> {

    Page<TaskNotificationEntity> findByRecipient_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<TaskNotificationEntity> findByRecipient_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<TaskNotificationEntity> findByIdAndRecipient_Id(Long id, Long userId);

    long countByRecipient_IdAndIsReadFalse(Long userId);

    void deleteByIdAndRecipient_Id(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM TaskNotification n WHERE n.recipient.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE TaskNotification n SET n.isRead = true, n.readAt = :now WHERE n.recipient.id = :userId AND n.isRead = false")
    void markAllReadByUserId(@Param("userId") Long userId, @Param("now") Date now);
}
