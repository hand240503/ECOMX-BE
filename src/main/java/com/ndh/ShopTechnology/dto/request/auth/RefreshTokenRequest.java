package com.ndh.ShopTechnology.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;

    @Size(max = 128, message = "deviceId không được vượt quá 128 ký tự")
    private String deviceId;
}