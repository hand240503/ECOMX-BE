package com.ndh.ShopTechnology.dto.request.task;

import com.ndh.ShopTechnology.enums.task.TaskPriority;
import com.ndh.ShopTechnology.enums.task.TaskType;
import lombok.Data;
import java.util.Date;

@Data
public class UpdateTaskRequest {
    private String title;
    private String description;
    private TaskType taskType;
    private Long departmentId;
    private TaskPriority priority;
    private Long assigneeId;
    private Date dueDate;
}
