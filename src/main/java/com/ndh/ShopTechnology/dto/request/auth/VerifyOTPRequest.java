package com.ndh.ShopTechnology.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyOTPRequest {

    @NotBlank(message = "Email hoặc số điện thoại không được để trống")
    private String login;

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(regexp = "\\d{6}", message = "Mã OTP phải là 6 chữ số")
    private String otp;
}