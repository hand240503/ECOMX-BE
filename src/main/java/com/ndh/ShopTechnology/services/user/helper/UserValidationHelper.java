package com.ndh.ShopTechnology.services.user.helper;

import org.springframework.stereotype.Component;

@Component
public class UserValidationHelper {

    public String validateAndNormalizeUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        return username.toLowerCase().trim();
    }

    public String validateAndNormalizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        return phone.trim();
    }

    public String validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        String trimmed = password.trim();
        if (trimmed.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        return trimmed;
    }
}
