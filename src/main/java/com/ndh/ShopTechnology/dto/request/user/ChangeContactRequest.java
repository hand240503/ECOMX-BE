package com.ndh.ShopTechnology.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeContactRequest {
    private String email;
    private String phoneNumber;

    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    private String currentPassword;
}

