package com.ndh.ShopTechnology.controller.auth;

import com.ndh.ShopTechnology.dto.request.auth.SendOTPRequest;
import com.ndh.ShopTechnology.dto.request.auth.VerifyOTPRequest;
import com.ndh.ShopTechnology.dto.response.APIResponse;
import com.ndh.ShopTechnology.services.otp.OTPService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/otp")
@RequiredArgsConstructor
public class OTPController {

    private final OTPService otpService;

    /**
     * Gửi mã OTP đến email/phone (Async)
     * POST /api/v1/auth/otp/send
     */
    @PostMapping("/send")
    public CompletableFuture<ResponseEntity<APIResponse<Void>>> sendOTP(
            @Valid @RequestBody SendOTPRequest request) {

        log.info("Received send OTP request for login: {}", request.getLogin());

        return CompletableFuture.supplyAsync(() -> {
            try {
                otpService.sendOTP(request);
                return ResponseEntity.ok(
                        APIResponse.of(
                                true,
                                "Mã xác thực đã được gửi thành công",
                                null,
                                null,
                                null
                        )
                );
            } catch (Exception ex) {
                log.error("Failed to send OTP for login: {}", request.getLogin(), ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(APIResponse.of(
                                false,
                                "Không thể gửi mã xác thực. Vui lòng thử lại sau.",
                                null,
                                null,
                                null
                        ));
            }
        });
    }

    /**
     * Xác thực mã OTP
     * POST /api/v1/auth/otp/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<APIResponse<Boolean>> verifyOTP(
            @Valid @RequestBody VerifyOTPRequest request) {

        log.info("Received verify OTP request for login: {}", request.getLogin());

        boolean isValid = otpService.verifyOTP(request.getLogin(), request.getOtp());

        if (!isValid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.of(
                            false,
                            "Mã xác thực không đúng hoặc đã hết hạn",
                            false,
                            null,
                            null
                    ));
        }

        return ResponseEntity.ok(
                APIResponse.of(
                        true,
                        "Xác thực thành công",
                        true,
                        null,
                        null
                )
        );
    }
}