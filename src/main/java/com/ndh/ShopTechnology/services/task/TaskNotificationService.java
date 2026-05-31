package com.ndh.ShopTechnology.services.task;

import com.ndh.ShopTechnology.entities.task.TaskEntity;
import com.ndh.ShopTechnology.entities.task.TaskNotificationEntity;
import com.ndh.ShopTechnology.entities.user.UserEntity;
import com.ndh.ShopTechnology.repository.task.TaskNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskNotificationService {

    private final TaskNotificationRepository notificationRepository;

    public static final String TYPE_ASSIGNED        = "ASSIGNED";
    public static final String TYPE_STATUS_CHANGED  = "STATUS_CHANGED";
    public static final String TYPE_COMMENTED       = "COMMENT_ADDED";
    public static final String TYPE_MENTIONED       = "MENTIONED";
    public static final String TYPE_DUE_APPROACHING = "DUE_DATE_APPROACHING";
    public static final String TYPE_ATTACHMENT      = "ATTACHMENT_ADDED";

    // ─── Tạo thông báo ───────────────────────────────────────────────────────

    @Async
    public void notifyAssigned(TaskEntity task, UserEntity assignee, UserEntity actor) {
        if (assignee == null || assignee.getId().equals(actor.getId())) return;
        send(assignee, task, TYPE_ASSIGNED,
            "Bạn được gán task: " + task.getTitle(),
            actor.getUsername() + " đã gán task này cho bạn");
    }

    @Async
    public void notifyStatusChanged(TaskEntity task, String oldStatus, String newStatus,
                                    UserEntity actor, List<UserEntity> participants) {
        String title = String.format("[%s] Trạng thái task thay đổi", task.getTitle());
        String body  = String.format("%s → %s (bởi %s)", oldStatus, newStatus, actor.getUsername());
        participants.stream()
            .filter(p -> !p.getId().equals(actor.getId()))
            .forEach(p -> send(p, task, TYPE_STATUS_CHANGED, title, body));
    }

    @Async
    public void notifyCommented(TaskEntity task, UserEntity commenter, List<UserEntity> participants) {
        String title = "Bình luận mới trong: " + task.getTitle();
        String body  = commenter.getUsername() + " đã thêm bình luận";
        participants.stream()
            .filter(p -> !p.getId().equals(commenter.getId()))
            .forEach(p -> send(p, task, TYPE_COMMENTED, title, body));
    }

    @Async
    public void notifyAttachmentAdded(TaskEntity task, UserEntity uploader, List<UserEntity> participants, String fileName) {
        String title = "Tệp mới trong: " + task.getTitle();
        String body  = uploader.getUsername() + " đã đính kèm tệp: " + fileName;
        participants.stream()
            .filter(p -> !p.getId().equals(uploader.getId()))
            .forEach(p -> send(p, task, TYPE_ATTACHMENT, title, body));
    }

    @Async
    public void notifyMentioned(TaskEntity task, UserEntity mentioned, UserEntity mentioner) {
        if (mentioned == null || mentioned.getId().equals(mentioner.getId())) return;
        send(mentioned, task, TYPE_MENTIONED,
            "Bạn được nhắc đến trong: " + task.getTitle(),
            mentioner.getUsername() + " đã @mention bạn trong bình luận");
    }

    // ─── API ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TaskNotificationEntity> getUnread(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipient_IdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskNotificationEntity> getAll(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        notificationRepository.findByIdAndRecipient_Id(notificationId, userId).ifPresent(n -> {
            n.setIsRead(true);
            n.setReadAt(new Date());
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId, new Date());
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByRecipient_IdAndIsReadFalse(userId);
    }

    @Transactional
    public void delete(Long notificationId, Long userId) {
        notificationRepository.deleteByIdAndRecipient_Id(notificationId, userId);
    }

    @Transactional
    public void deleteAll(Long userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private void send(UserEntity recipient, TaskEntity task,
                      String type, String title, String body) {
        try {
            notificationRepository.save(TaskNotificationEntity.builder()
                .recipient(recipient)
                .task(task)
                .notificationType(type)
                .title(title)
                .body(body)
                .build());
        } catch (Exception ex) {
            log.warn("Failed to send notification to user {}: {}", recipient.getId(), ex.getMessage());
        }
    }
}
