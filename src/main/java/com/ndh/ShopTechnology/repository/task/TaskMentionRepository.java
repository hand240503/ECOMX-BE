package com.ndh.ShopTechnology.repository.task;

import com.ndh.ShopTechnology.entities.task.TaskMentionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskMentionRepository extends JpaRepository<TaskMentionEntity, Long> {

    List<TaskMentionEntity> findByComment_Id(Long commentId);

    boolean existsByComment_IdAndMentionedUser_Id(Long commentId, Long userId);

    long countByMentionedUser_IdAndIsReadFalse(Long userId);
}
