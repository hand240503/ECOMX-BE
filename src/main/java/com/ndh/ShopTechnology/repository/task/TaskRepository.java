package com.ndh.ShopTechnology.repository.task;

import com.ndh.ShopTechnology.entities.task.TaskEntity;
import com.ndh.ShopTechnology.enums.task.TaskStatus;
import com.ndh.ShopTechnology.enums.task.TaskSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    /* ── Kanban board queries ── */

    @EntityGraph(attributePaths = {"creator", "assignee"})
    List<TaskEntity> findByBoardIdAndIsDeletedFalseOrderByStatusAscPositionAsc(Long boardId);

    @EntityGraph(attributePaths = {"creator", "assignee"})
    List<TaskEntity> findByBoardIdAndStatusAndIsDeletedFalseOrderByPositionAsc(Long boardId, TaskStatus status);

    @Query("SELECT MAX(t.position) FROM Task t WHERE t.boardId = :boardId AND t.status = :status AND t.isDeleted = false")
    Optional<Integer> findMaxPositionByBoardIdAndStatus(@Param("boardId") Long boardId, @Param("status") TaskStatus status);

    /* ── Visibility-filtered queries ── */

    /** Only tasks assigned to a specific user (for staff without order management permission) */
    @EntityGraph(attributePaths = {"creator", "assignee"})
    List<TaskEntity> findByBoardIdAndAssignee_IdAndIsDeletedFalseOrderByStatusAscPositionAsc(
        Long boardId, Long assigneeId);

    @EntityGraph(attributePaths = {"creator", "assignee"})
    @Query("SELECT t FROM Task t WHERE t.boardId = :boardId AND t.isDeleted = false AND (" +
           "t.assignee.id = :userId OR " +
           "t.departmentId IS NULL OR " +
           "t.departmentId IN :departmentIds) " +
           "ORDER BY t.status ASC, t.position ASC")
    List<TaskEntity> findVisibleTasksForUser(
        @Param("boardId") Long boardId,
        @Param("userId") Long userId,
        @Param("departmentIds") List<Long> departmentIds);

    /* ── Search & filter ── */

    @EntityGraph(attributePaths = {"creator", "assignee"})
    Page<TaskEntity> findByBoardIdAndIsDeletedFalse(Long boardId, Pageable pageable);

    @EntityGraph(attributePaths = {"creator", "assignee"})
    Page<TaskEntity> findByBoardIdAndStatusAndIsDeletedFalse(Long boardId, TaskStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"creator", "assignee"})
    Page<TaskEntity> findByAssigneeIdAndIsDeletedFalse(Long assigneeId, Pageable pageable);

    /* ── Dashboard queries ── */

    long countByBoardIdAndIsDeletedFalse(Long boardId);

    long countByBoardIdAndStatusAndIsDeletedFalse(Long boardId, TaskStatus status);

    long countByBoardIdAndDueDateBeforeAndStatusNotInAndIsDeletedFalse(
        Long boardId, Date now, List<TaskStatus> excludedStatuses);

    @Query("""
        SELECT COUNT(t) FROM Task t
        WHERE t.boardId = :boardId
          AND t.isDeleted = false
          AND t.status = 'DONE'
          AND t.completedAt >= :startOfDay
        """)
    long countCompletedTodayByBoardId(
        @Param("boardId") Long boardId,
        @Param("startOfDay") Date startOfDay);

    @Query("""
        SELECT t FROM Task t
        WHERE t.boardId = :boardId
          AND t.isDeleted = false
          AND t.dueDate < :now
          AND t.status NOT IN (:excludedStatuses)
        ORDER BY t.dueDate ASC
        """)
    List<TaskEntity> findOverdueTasks(
        @Param("boardId") Long boardId,
        @Param("now") Date now,
        @Param("excludedStatuses") List<TaskStatus> excludedStatuses);

    /* ── Source integration ── */

    Optional<TaskEntity> findBySourceTypeAndSourceIdAndIsDeletedFalse(TaskSourceType sourceType, Long sourceId);

    boolean existsBySourceTypeAndSourceIdAndIsDeletedFalse(TaskSourceType sourceType, Long sourceId);

    /* ── Position management ── */

    @Modifying
    @Query("""
        UPDATE Task t SET t.position = t.position + 1
        WHERE t.boardId = :boardId AND t.status = :status
          AND t.position >= :fromPosition AND t.isDeleted = false
        """)
    void shiftPositionsDown(@Param("boardId") Long boardId, @Param("status") TaskStatus status,
                            @Param("fromPosition") int fromPosition);
}
