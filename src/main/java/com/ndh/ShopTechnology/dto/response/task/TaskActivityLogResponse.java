package com.ndh.ShopTechnology.dto.response.task;

import com.ndh.ShopTechnology.entities.task.TaskActivityLogEntity;
import com.ndh.ShopTechnology.enums.task.TaskActivityAction;
import lombok.Builder;
import lombok.Data;
import java.util.Date;
import java.util.Map;

@Data @Builder
public class TaskActivityLogResponse {
    private Long id;
    private TaskResponse.AssigneeInfo actor;
    private TaskActivityAction actionType;
    private Map<String, Object> oldValue;
    private Map<String, Object> newValue;
    private Date createdAt;

    public static TaskActivityLogResponse fromEntity(TaskActivityLogEntity e) {
        return TaskActivityLogResponse.builder()
            .id(e.getId())
            .actor(e.getActor() == null ? null : TaskResponse.AssigneeInfo.builder()
                .id(e.getActor().getId())
                .username(e.getActor().getUsername())
                .fullName(e.getActor().getUserInfo() != null ? e.getActor().getUserInfo().getFullName() : null)
                .avatar(e.getActor().getUserInfo() != null ? e.getActor().getUserInfo().getAvatar() : null)
                .build())
            .actionType(e.getActionType())
            .oldValue(e.getOldValue())
            .newValue(e.getNewValue())
            .createdAt(e.getCreatedAt())
            .build();
    }
}
