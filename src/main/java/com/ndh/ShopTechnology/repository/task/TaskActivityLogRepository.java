package com.ndh.ShopTechnology.repository.task;

import com.ndh.ShopTechnology.entities.task.TaskActivityLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskActivityLogRepository extends JpaRepository<TaskActivityLogEntity, Long> {

    @EntityGraph(attributePaths = {"actor"})
    Page<TaskActivityLogEntity> findByTask_IdOrderByCreatedAtDesc(Long taskId, Pageable pageable);
}
