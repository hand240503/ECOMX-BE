package com.ndh.ShopTechnology.controller.admin.task;

import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.task.TaskNotificationResponse;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.exception.NotFoundEntityException;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.task.TaskNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/admin/tasks/notifications")
@RequiredArgsConstructor
public class TaskNotificationController {

    private final TaskNotificationService notificationService;
    private final UserRepository userRepository;

    /** Số thông báo chưa đọc — dùng cho badge trên UI */
    @GetMapping("/unread-count")
    public ResponseEntity<APIResponse<Long>> countUnread() {
        long count = notificationService.countUnread(currentUserId());
        return ResponseEntity.ok(APIResponse.of(true, "OK", count, null, null));
    }

    /** Danh sách thông báo chưa đọc */
    @GetMapping("/unread")
    public ResponseEntity<APIResponse<Page<TaskNotificationResponse>>> getUnread(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TaskNotificationResponse> result = notificationService
            .getUnread(currentUserId(), PageRequest.of(page, size))
            .map(TaskNotificationResponse::fromEntity);
        return ResponseEntity.ok(APIResponse.of(true, "OK", result, null, null));
    }

    /** Toàn bộ thông báo (đọc + chưa đọc) */
    @GetMapping("")
    public ResponseEntity<APIResponse<Page<TaskNotificationResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TaskNotificationResponse> result = notificationService
            .getAll(currentUserId(), PageRequest.of(page, size))
            .map(TaskNotificationResponse::fromEntity);
        return ResponseEntity.ok(APIResponse.of(true, "OK", result, null, null));
    }

    /** Đánh dấu 1 thông báo đã đọc */
    @PatchMapping("/{id}/read")
    public ResponseEntity<APIResponse<Void>> markRead(@PathVariable Long id) {
        notificationService.markRead(id, currentUserId());
        return ResponseEntity.ok(APIResponse.of(true, "Đã đánh dấu đã đọc", null, null, null));
    }

    /** Đánh dấu tất cả đã đọc */
    @PatchMapping("/read-all")
    public ResponseEntity<APIResponse<Void>> markAllRead() {
        notificationService.markAllRead(currentUserId());
        return ResponseEntity.ok(APIResponse.of(true, "Đã đọc tất cả", null, null, null));
    }

    /** Xóa 1 thông báo */
    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteNotification(@PathVariable Long id) {
        notificationService.delete(id, currentUserId());
        return ResponseEntity.ok(APIResponse.of(true, "Đã xóa thông báo", null, null, null));
    }

    /** Xóa tất cả thông báo */
    @DeleteMapping("/all")
    public ResponseEntity<APIResponse<Void>> deleteAllNotifications() {
        notificationService.deleteAll(currentUserId());
        return ResponseEntity.ok(APIResponse.of(true, "Đã xóa tất cả thông báo", null, null, null));
    }

    private Long currentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findOneByUsername(username)
            .map(UserEntity::getId)
            .orElseThrow(() -> new NotFoundEntityException("User không tồn tại"));
    }
}
