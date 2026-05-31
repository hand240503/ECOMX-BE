package com.ndh.ShopTechnology.services.task;

import com.ndh.ShopTechnology.dto.request.task.*;
import com.ndh.ShopTechnology.dto.response.task.*;
import com.ndh.ShopTechnology.enums.task.TaskSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    /** Lấy board mặc định */
    KanbanBoardResponse getDefaultBoard();

    /** Lấy toàn bộ board Kanban (tất cả cột) */
    KanbanBoardResponse getKanbanBoard(Long boardId);

    /** CRUD Task */
    TaskResponse createTask(CreateTaskRequest request);
    TaskResponse getTaskById(Long taskId);
    TaskResponse updateTask(Long taskId, UpdateTaskRequest request);
    void deleteTask(Long taskId);

    /** Kéo thả — chuyển task sang cột khác và cập nhật position */
    TaskResponse moveTask(Long taskId, MoveTaskRequest request);

    /** Gán người phụ trách */
    TaskResponse assignTask(Long taskId, Long assigneeId);

    /** Tự động tạo task từ nguồn bên ngoài (Order, PriceChange, ...) */
    TaskResponse autoCreateFromSource(TaskSourceType sourceType, Long sourceId, Long boardId,
                                      String titleOverride, String descriptionOverride);

    /**
     * Chuyển task liên kết với nguồn sang trạng thái CANCELLED.
     * Dùng khi đơn hàng bị hủy để tự động đóng task tương ứng.
     * Không ném exception nếu task không tồn tại (idempotent).
     */
    void cancelTaskBySource(TaskSourceType sourceType, Long sourceId);

    /** Comments */
    TaskCommentResponse addComment(Long taskId, AddCommentRequest request);
    Page<TaskCommentResponse> getComments(Long taskId, Pageable pageable);
    void deleteComment(Long commentId);

    /** Checklist */
    TaskResponse addChecklist(Long taskId, AddChecklistRequest request);
    TaskResponse toggleChecklistItem(Long itemId);

    /** Activity log */
    Page<TaskActivityLogResponse> getActivityLog(Long taskId, Pageable pageable);

    /** Dashboard */
    KanbanBoardResponse.DashboardStats getDashboardStats(Long boardId);
}
