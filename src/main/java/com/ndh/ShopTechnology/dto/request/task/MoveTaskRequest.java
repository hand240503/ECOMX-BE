package com.ndh.ShopTechnology.dto.request.task;

import com.ndh.ShopTechnology.enums.task.TaskStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveTaskRequest {
    @NotNull
    private TaskStatus targetStatus;

    @NotNull
    @Min(0)
    private Integer targetPosition;

    private String note;
}
