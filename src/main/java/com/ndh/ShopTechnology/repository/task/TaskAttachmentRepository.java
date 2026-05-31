package com.ndh.ShopTechnology.repository.task;

import com.ndh.ShopTechnology.entities.task.TaskAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachmentEntity, Long> {
    List<TaskAttachmentEntity> findByTask_IdAndIsDeletedFalse(Long taskId);
    long countByTask_IdAndIsDeletedFalse(Long taskId);
}
