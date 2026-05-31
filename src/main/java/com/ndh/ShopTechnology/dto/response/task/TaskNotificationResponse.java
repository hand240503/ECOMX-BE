package com.ndh.ShopTechnology.dto.response.task;

import com.ndh.ShopTechnology.entities.task.TaskNotificationEntity;
import lombok.Builder;
import lombok.Data;
import java.util.Date;

@Data @Builder
public class TaskNotificationResponse {
    private Long id;
    private Long taskId;
    private String taskTitle;
    private String notificationType;
    private String title;
    private String body;
    private Boolean isRead;
    private Date readAt;
    private Date createdAt;

    public static TaskNotificationResponse fromEntity(TaskNotificationEntity e) {
        return TaskNotificationResponse.builder()
            .id(e.getId())
            .taskId(e.getTask() != null ? e.getTask().getId() : null)
            .taskTitle(e.getTask() != null ? e.getTask().getTitle() : null)
            .notificationType(e.getNotificationType())
            .title(e.getTitle())
            .body(e.getBody())
            .isRead(e.getIsRead())
            .readAt(e.getReadAt())
            .createdAt(e.getCreatedAt())
            .build();
    }
}
