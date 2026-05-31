package com.ndh.ShopTechnology.dto.request.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class CreateDepartmentRequest {

    @NotBlank(message = "Tên phòng ban không được để trống")
    @Size(max = 100, message = "Tên phòng ban tối đa 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    /** Hex color, e.g. #3B82F6 */
    private String color;

    /** Danh sách permission code cấp cho thành viên */
    private Set<Integer> permissionCodes;
}
