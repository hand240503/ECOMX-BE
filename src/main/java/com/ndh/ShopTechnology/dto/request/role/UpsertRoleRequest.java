package com.ndh.ShopTechnology.dto.request.role;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

/**
 * Tạo / cập nhật 1 role và danh sách permission mặc định của role đó.
 *
 * <p>Người gọi phải có quyền {@link com.ndh.ShopTechnology.constants.PermissionCode#MANAGE_ROLE}.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpsertRoleRequest {

    @NotBlank(message = "code is required")
    private String code;

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    private Integer status;

    private Set<Integer> permissionCodes;
}
