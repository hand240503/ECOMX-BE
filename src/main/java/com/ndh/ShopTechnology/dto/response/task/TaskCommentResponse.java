package com.ndh.ShopTechnology.dto.response.task;

import com.ndh.ShopTechnology.entities.task.TaskCommentEntity;
import lombok.Builder;
import lombok.Data;
import java.util.Date;

@Data @Builder
public class TaskCommentResponse {
    private Long id;
    private Long taskId;
    private TaskResponse.AssigneeInfo author;
    private Long parentId;
    private String content;
    private Boolean isEdited;
    private Date editedAt;
    private Date createdDate;

    public static TaskCommentResponse fromEntity(TaskCommentEntity e) {
        if (e == null) return null;
        return TaskCommentResponse.builder()
            .id(e.getId())
            .taskId(e.getTask() != null ? e.getTask().getId() : null)
            .author(e.getAuthor() == null ? null : TaskResponse.AssigneeInfo.builder()
                .id(e.getAuthor().getId())
                .username(e.getAuthor().getUsername())
                .fullName(e.getAuthor().getUserInfo() != null ? e.getAuthor().getUserInfo().getFullName() : null)
                .avatar(e.getAuthor().getUserInfo() != null ? e.getAuthor().getUserInfo().getAvatar() : null)
                .build())
            .parentId(e.getParent() != null ? e.getParent().getId() : null)
            .content(e.getContent())
            .isEdited(e.getIsEdited())
            .editedAt(e.getEditedAt())
            .createdDate(e.getCreatedDate())
            .build();
    }
}
