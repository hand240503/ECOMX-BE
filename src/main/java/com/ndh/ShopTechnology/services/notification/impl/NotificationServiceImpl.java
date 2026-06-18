package com.ndh.ShopTechnology.services.notification.impl;

import com.ndh.ShopTechnology.dto.response.notification.NotificationResponse;
import com.ndh.ShopTechnology.entities.notification.NotificationEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.repository.NotificationRepository;
import com.ndh.ShopTechnology.services.notification.NotificationService;
import com.ndh.ShopTechnology.services.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final int MAX_LIMIT = 50;

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyUser(UserEntity user, String type, String title, String message, Long orderId) {
        if (user == null || title == null) {
            return;
        }
        try {
            NotificationEntity n = NotificationEntity.builder()
                    .user(user)
                    .type(type)
                    .title(title)
                    .message(message)
                    .orderId(orderId)
                    .isRead(false)
                    .build();
            notificationRepository.save(n);
        } catch (Exception e) {
            // Không để lỗi thông báo làm hỏng luồng cập nhật đơn hàng.
            log.warn("Failed to create notification for userId={}: {}",
                    user.getId(), e.toString());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(int limit) {
        UserEntity me = userService.getCurrentUser();
        int size = Math.max(1, Math.min(limit <= 0 ? 20 : limit, MAX_LIMIT));
        List<NotificationEntity> list = notificationRepository
                .findByUser_IdOrderByIdDesc(me.getId(), PageRequest.of(0, size));
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream().map(NotificationResponse::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getMyUnreadCount() {
        UserEntity me = userService.getCurrentUser();
        return notificationRepository.countByUser_IdAndIsReadFalse(me.getId());
    }

    @Override
    @Transactional
    public void markRead(Long id) {
        UserEntity me = userService.getCurrentUser();
        notificationRepository.findByIdAndUser_Id(id, me.getId()).ifPresent(n -> {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Override
    @Transactional
    public void markAllRead() {
        UserEntity me = userService.getCurrentUser();
        notificationRepository.markAllReadByUser(me.getId());
    }
}
