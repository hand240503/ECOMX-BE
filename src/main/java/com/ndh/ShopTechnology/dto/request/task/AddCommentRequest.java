package com.ndh.ShopTechnology.dto.request.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddCommentRequest {
    @NotBlank(message = "content is required")
    private String content;

    private Long parentId;
}
