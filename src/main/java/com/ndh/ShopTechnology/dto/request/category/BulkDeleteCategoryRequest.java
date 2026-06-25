package com.ndh.ShopTechnology.dto.request.category;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** Yêu cầu xóa danh mục hàng loạt. */
@Data
public class BulkDeleteCategoryRequest {

    @NotEmpty(message = "Danh sách danh mục cần xóa không được rỗng")
    private List<Long> ids;
}
