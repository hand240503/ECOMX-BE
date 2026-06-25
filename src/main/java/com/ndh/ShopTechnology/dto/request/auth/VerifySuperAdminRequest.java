package com.ndh.ShopTechnology.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Body cho POST {api.prefix}/admin/products/verify-super-admin — xác thực mật khẩu super admin. */
@Getter
@Setter
public class VerifySuperAdminRequest {

    @NotBlank(message = "Password is required")
    private String password;
}
