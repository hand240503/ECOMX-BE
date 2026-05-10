package com.ndh.ShopTechnology.dto.request.permission;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RevokePermissionRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotEmpty(message = "permissionCodes must not be empty")
    private List<Integer> permissionCodes;
}
