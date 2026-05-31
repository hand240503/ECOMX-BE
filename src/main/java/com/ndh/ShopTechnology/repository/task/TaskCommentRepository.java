package com.ndh.ShopTechnology.repository.task;

import com.ndh.ShopTechnology.entities.task.TaskCommentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskCommentEntity, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<TaskCommentEntity> findByTask_IdAndIsDeletedFalseAndParentIsNullOrderByCreatedDateAsc(
        Long taskId, Pageable pageable);

    long countByTask_IdAndIsDeletedFalse(Long taskId);
}
