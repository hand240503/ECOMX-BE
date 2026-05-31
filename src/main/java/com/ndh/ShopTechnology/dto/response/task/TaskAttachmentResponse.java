package com.ndh.ShopTechnology.dto.response.task;

import com.ndh.ShopTechnology.entities.task.TaskAttachmentEntity;
import lombok.Builder;
import lombok.Data;
import java.util.Date;

@Data @Builder
public class TaskAttachmentResponse {
    private Long id;
    private Long taskId;
    private Long commentId;
    private TaskResponse.AssigneeInfo uploader;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private Date createdAt;

    public static TaskAttachmentResponse fromEntity(TaskAttachmentEntity e) {
        return TaskAttachmentResponse.builder()
            .id(e.getId())
            .taskId(e.getTask() != null ? e.getTask().getId() : null)
            .commentId(e.getComment() != null ? e.getComment().getId() : null)
            .uploader(e.getUploader() == null ? null : TaskResponse.AssigneeInfo.builder()
                .id(e.getUploader().getId())
                .username(e.getUploader().getUsername())
                .fullName(e.getUploader().getUserInfo() != null ? e.getUploader().getUserInfo().getFullName() : null)
                .avatar(e.getUploader().getUserInfo() != null ? e.getUploader().getUserInfo().getAvatar() : null)
                .build())
            .fileName(e.getFileName())
            .filePath(e.getFilePath())
            .fileSize(e.getFileSize())
            .mimeType(e.getMimeType())
            .createdAt(e.getCreatedAt())
            .build();
    }
}
