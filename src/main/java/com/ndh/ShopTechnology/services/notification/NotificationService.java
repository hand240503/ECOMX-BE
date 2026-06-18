package com.ndh.ShopTechnology.services.notification;

import com.ndh.ShopTechnology.dto.response.notification.NotificationResponse;
import com.ndh.ShopTechnology.entities.user.UserEntity;

import java.util.List;

public interface NotificationService {

    /** Tạo thông báo cho 1 user. Không bao giờ ném lỗi ra ngoài (không làm hỏng luồng chính). */
    void notifyUser(UserEntity user, String type, String title, String message, Long orderId);

    /** Danh sách thông báo mới nhất của user hiện tại. */
    List<NotificationResponse> getMyNotifications(int limit);

    /** Số thông báo chưa đọc của user hiện tại. */
    long getMyUnreadCount();

    /** Đánh dấu 1 thông báo đã đọc (của user hiện tại). */
    void markRead(Long id);

    /** Đánh dấu tất cả đã đọc. */
    void markAllRead();
}
