package com.ndh.ShopTechnology.dto.request.task;

import com.ndh.ShopTechnology.enums.task.TaskPriority;
import com.ndh.ShopTechnology.enums.task.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Date;

@Data
public class CreateTaskRequest {
    @NotNull(message = "boardId is required")
    private Long boardId;

    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    private String description;

    @NotNull(message = "taskType is required")
    private TaskType taskType;

    private Long departmentId;

    @NotNull(message = "priority is required")
    private TaskPriority priority;

    private Long assigneeId;
    private Date dueDate;

    /** Danh sách user_id tham gia */
    private java.util.List<Long> participantIds;
}
