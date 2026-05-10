package com.ndh.ShopTechnology.dto.request.permission;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Yêu cầu cấp quyền thêm cho 1 user.
 *
 * <p>Người gọi phải có quyền {@link com.ndh.ShopTechnology.constants.PermissionCode#GRANT_PERMISSION}
 * và <b>chỉ được cấp những quyền mình đang có</b> (kiểm tra ở service).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GrantPermissionRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotEmpty(message = "permissionCodes must not be empty")
    private List<Integer> permissionCodes;

    /** Thời điểm hết hạn (null = vô thời hạn). */
    private LocalDateTime expiresAt;
}
