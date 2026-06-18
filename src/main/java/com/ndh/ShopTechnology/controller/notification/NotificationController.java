package com.ndh.ShopTechnology.controller.notification;

import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.dto.response.notification.NotificationResponse;
import com.ndh.ShopTechnology.services.notification.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.prefix}/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Danh sách thông báo mới nhất của user hiện tại. */
    @GetMapping
    public ResponseEntity<APIResponse<List<NotificationResponse>>> myNotifications(
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        List<NotificationResponse> data = notificationService.getMyNotifications(limit);
        return ResponseEntity.ok(APIResponse.of(true, "OK", data, null, null));
    }

    /** Số thông báo chưa đọc. */
    @GetMapping("/unread-count")
    public ResponseEntity<APIResponse<Map<String, Long>>> unreadCount() {
        long count = notificationService.getMyUnreadCount();
        return ResponseEntity.ok(APIResponse.of(true, "OK", Map.of("unreadCount", count), null, null));
    }

    /** Đánh dấu 1 thông báo đã đọc. */
    @PostMapping("/{id}/read")
    public ResponseEntity<APIResponse<Void>> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.ok(APIResponse.of(true, "OK", null, null, null));
    }

    /** Đánh dấu tất cả đã đọc. */
    @PostMapping("/read-all")
    public ResponseEntity<APIResponse<Void>> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.ok(APIResponse.of(true, "OK", null, null, null));
    }
}
