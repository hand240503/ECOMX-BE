package com.ndh.ShopTechnology.dto.request.comment;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request cập nhật comment từ phía admin — có thể sửa nội dung và trạng thái ẩn/hiện.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUpdateProductCommentRequest {

    @Size(max = 2000, message = "Nội dung bình luận không vượt quá 2000 ký tự")
    private String content;

    /** Ẩn comment khỏi hiển thị phía khách hàng. Null = không thay đổi. */
    private Boolean isHidden;
}
