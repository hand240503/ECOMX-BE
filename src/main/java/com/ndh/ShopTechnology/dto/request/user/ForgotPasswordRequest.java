package com.ndh.ShopTechnology.dto.request.user;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForgotPasswordRequest {
    private String email;
    private int code;
    private String password;
    private String rePassword;;
}
