package com.ndh.ShopTechnology.repository.task;

import com.ndh.ShopTechnology.entities.task.TaskParticipantEntity;
import com.ndh.ShopTechnology.enums.task.TaskParticipantRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskParticipantRepository extends JpaRepository<TaskParticipantEntity, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<TaskParticipantEntity> findByTask_Id(Long taskId);

    Optional<TaskParticipantEntity> findByTask_IdAndUser_IdAndRole(Long taskId, Long userId, TaskParticipantRole role);

    void deleteByTask_IdAndUser_Id(Long taskId, Long userId);
}
