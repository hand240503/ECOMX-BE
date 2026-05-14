package com.ndh.ShopTechnology.dto.request.user;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminModUserInfoRequest {

    private Long id;

    // Core fields
    private String password;
    private String email;
    private String phoneNumber;
    private Integer status;
    private Integer type;

    /** Quản lý trực tiếp (cột users.man_id), admin/manager hạ tầng. */
    private Long manId;

    // User info fields
    private String fullName;
    private String telephone;
    private String avatar;
    private Long managerId;

    // Custom fields
    private String info01;
    private String info02;
    private String info03;
    private String info04;

    // Role (đơn)
    private Long roleId;

    /** Cấp thêm quyền cho user (cùng cơ chế {@code POST .../permissions/grant}). */
    private List<Integer> grantPermissionCodes;

    /** Thu hồi quyền cấp thêm (không ảnh hưởng quyền gán sẵn theo role). */
    private List<Integer> revokePermissionCodes;

    /** Hạn cấp quyền (áp dụng cho toàn bộ mã trong {@link #grantPermissionCodes} trong lần cập nhật này). */
    private LocalDateTime permissionGrantExpiresAt;
}

