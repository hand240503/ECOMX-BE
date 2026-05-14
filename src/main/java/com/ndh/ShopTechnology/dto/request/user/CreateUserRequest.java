package com.ndh.ShopTechnology.dto.request.user;

import lombok.*;

// ========== CreateUserRequest ==========
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateUserRequest {

    // Core fields
    private String username;
    private String password;
    private String email;
    private String phoneNumber;

    // User info fields
    private String fullName;
    private String telephone;
    private String avatar;
    private Long managerId;

    /** Gán users.man_id (quản lý trực tiếp). Chỉ admin/SUPER_ADMIN truyền; manager luôn gán theo người tạo. */
    private Long manId;

    // Custom fields
    private String info01;
    private String info02;
    private String info03;
    private String info04;

    // Role (đơn — id trong bảng roles)
    private Long roleId;
}
