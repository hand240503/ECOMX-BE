package com.ndh.ShopTechnology.repository.task;

import com.ndh.ShopTechnology.entities.task.TaskStatusHistoryEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskStatusHistoryRepository extends JpaRepository<TaskStatusHistoryEntity, Long> {

    @EntityGraph(attributePaths = {"changedBy"})
    List<TaskStatusHistoryEntity> findByTask_IdOrderByChangedAtAsc(Long taskId);
}
