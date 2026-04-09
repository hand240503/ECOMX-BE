package com.ndh.ShopTechnology.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordResponse {
    private boolean success;
    private int status;
    private String message;

    public static ChangePasswordResponse of(boolean success, HttpStatus status, String message) {
        return ChangePasswordResponse.builder()
                .success(success)
                .status(status.value())
                .message(message)
                .build();
    }
}
