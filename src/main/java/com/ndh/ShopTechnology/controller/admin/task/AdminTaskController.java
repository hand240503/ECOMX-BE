package com.ndh.ShopTechnology.controller.admin.task;

import com.ndh.ShopTechnology.dto.request.task.*;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.task.*;
import com.ndh.ShopTechnology.services.task.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/admin/tasks")
@RequiredArgsConstructor
public class AdminTaskController {

    private final TaskService taskService;

    /* ── Board ── */

    /**
     * Lấy board theo ID hoặc dùng "default" để lấy board mặc định.
     * Chấp nhận cả /board/default và /board/{id} để tránh xung đột route.
     */
    @GetMapping("/board/{boardId}")
    public ResponseEntity<APIResponse<KanbanBoardResponse>> getKanbanBoard(
            @PathVariable String boardId) {
        KanbanBoardResponse response;
        if ("default".equalsIgnoreCase(boardId)) {
            response = taskService.getDefaultBoard();
        } else {
            try {
                response = taskService.getKanbanBoard(Long.parseLong(boardId));
            } catch (NumberFormatException e) {
                response = taskService.getDefaultBoard();
            }
        }
        return ResponseEntity.ok(APIResponse.of(true, "OK", response, null, null));
    }

    @GetMapping("/board/{boardId}/stats")
    public ResponseEntity<APIResponse<KanbanBoardResponse.DashboardStats>> getStats(
            @PathVariable String boardId) {
        KanbanBoardResponse.DashboardStats stats;
        if ("default".equalsIgnoreCase(boardId)) {
            KanbanBoardResponse board = taskService.getDefaultBoard();
            stats = taskService.getDashboardStats(board.getBoardId());
        } else {
            try {
                stats = taskService.getDashboardStats(Long.parseLong(boardId));
            } catch (NumberFormatException e) {
                KanbanBoardResponse board = taskService.getDefaultBoard();
                stats = taskService.getDashboardStats(board.getBoardId());
            }
        }
        return ResponseEntity.ok(APIResponse.of(true, "OK", stats, null, null));
    }

    /* ── Task CRUD ── */

    @PostMapping("")
    public ResponseEntity<APIResponse<TaskResponse>> createTask(
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(APIResponse.of(true, "Task đã tạo thành công", created, null, null));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<APIResponse<TaskResponse>> getTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(APIResponse.of(true, "OK", taskService.getTaskById(taskId), null, null));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<APIResponse<TaskResponse>> updateTask(
            @PathVariable Long taskId,
            @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(APIResponse.of(true, "Đã cập nhật", taskService.updateTask(taskId, request), null, null));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<APIResponse<Void>> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.ok(APIResponse.of(true, "Đã xóa", null, null, null));
    }

    /* ── Kanban move ── */

    @PatchMapping("/{taskId}/move")
    public ResponseEntity<APIResponse<TaskResponse>> moveTask(
            @PathVariable Long taskId,
            @Valid @RequestBody MoveTaskRequest request) {
        return ResponseEntity.ok(APIResponse.of(true, "Đã di chuyển", taskService.moveTask(taskId, request), null, null));
    }

    /* ── Assign ── */

    @PatchMapping("/{taskId}/assign/{userId}")
    public ResponseEntity<APIResponse<TaskResponse>> assignTask(
            @PathVariable Long taskId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(APIResponse.of(true, "Đã gán", taskService.assignTask(taskId, userId), null, null));
    }

    /* ── Comments ── */

    @PostMapping("/{taskId}/comments")
    public ResponseEntity<APIResponse<TaskCommentResponse>> addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody AddCommentRequest request) {
        TaskCommentResponse comment = taskService.addComment(taskId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(APIResponse.of(true, "Đã thêm bình luận", comment, null, null));
    }

    @GetMapping("/{taskId}/comments")
    public ResponseEntity<APIResponse<Page<TaskCommentResponse>>> getComments(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TaskCommentResponse> result = taskService.getComments(
            taskId, PageRequest.of(page, size, Sort.by("createdDate").ascending()));
        return ResponseEntity.ok(APIResponse.of(true, "OK", result, null, null));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<APIResponse<Void>> deleteComment(@PathVariable Long commentId) {
        taskService.deleteComment(commentId);
        return ResponseEntity.ok(APIResponse.of(true, "Đã xóa bình luận", null, null, null));
    }

    /* ── Checklist ── */

    @PostMapping("/{taskId}/checklists")
    public ResponseEntity<APIResponse<TaskResponse>> addChecklist(
            @PathVariable Long taskId,
            @Valid @RequestBody AddChecklistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(APIResponse.of(true, "Đã thêm checklist", taskService.addChecklist(taskId, request), null, null));
    }

    @PatchMapping("/checklist-items/{itemId}/toggle")
    public ResponseEntity<APIResponse<TaskResponse>> toggleItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(APIResponse.of(true, "OK", taskService.toggleChecklistItem(itemId), null, null));
    }

    /* ── Activity log ── */

    @GetMapping("/{taskId}/activity")
    public ResponseEntity<APIResponse<Page<TaskActivityLogResponse>>> getActivity(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TaskActivityLogResponse> logs = taskService.getActivityLog(
            taskId, PageRequest.of(page, size));
        return ResponseEntity.ok(APIResponse.of(true, "OK", logs, null, null));
    }
}
