package com.ndh.ShopTechnology.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String field;
    private String message;
    private Object rejectedValue;
}