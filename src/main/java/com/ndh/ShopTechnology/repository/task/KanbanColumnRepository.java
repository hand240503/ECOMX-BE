package com.ndh.ShopTechnology.repository.task;

import com.ndh.ShopTechnology.entities.task.KanbanColumnEntity;
import com.ndh.ShopTechnology.enums.task.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KanbanColumnRepository extends JpaRepository<KanbanColumnEntity, Long> {
    List<KanbanColumnEntity> findByBoardIdOrderByPositionAsc(Long boardId);
    boolean existsByBoardIdAndStatusKey(Long boardId, TaskStatus statusKey);
}
