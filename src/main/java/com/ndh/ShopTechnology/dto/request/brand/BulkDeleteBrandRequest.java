package com.ndh.ShopTechnology.dto.request.brand;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** Yêu cầu xóa thương hiệu hàng loạt. */
@Data
public class BulkDeleteBrandRequest {

    @NotEmpty(message = "Danh sách thương hiệu cần xóa không được rỗng")
    private List<Long> ids;
}
