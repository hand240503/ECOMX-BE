package com.ndh.ShopTechnology.repository.task;

import com.ndh.ShopTechnology.entities.task.KanbanBoardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface KanbanBoardRepository extends JpaRepository<KanbanBoardEntity, Long> {
    Optional<KanbanBoardEntity> findFirstByIsDefaultTrueAndIsActiveTrueAndIsDeletedFalse();
}
