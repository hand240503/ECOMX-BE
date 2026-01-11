
package com.ndh.ShopTechnology.services.otp;

import com.ndh.ShopTechnology.dto.request.auth.SendOTPRequest;

public interface OTPService {
    void sendOTP(SendOTPRequest request);

    boolean verifyOTP(String login, String otp);

    boolean verifyOTPForRegister(String login, String otp);

    void clearOTP(String login);
}