package com.ndh.ShopTechnology.dto.request.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SendOTPRequest {
    private String login;
}