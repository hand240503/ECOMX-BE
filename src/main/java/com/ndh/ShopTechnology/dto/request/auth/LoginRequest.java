package com.ndh.ShopTechnology.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LoginRequest {

    @NotBlank(message = "Username/Phone number is required")
    private String login;

    @NotBlank(message = "Password is required")
    private String password;
}
