package com.ndh.ShopTechnology.dto.request.comment;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUpdateProductCommentRequest {

    @Size(max = 2000, message = "Nội dung bình luận không vượt quá 2000 ký tự")
    private String content;

    private Boolean isHidden;
}
