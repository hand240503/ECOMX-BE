package com.ndh.ShopTechnology.services.otp.impl;

import com.ndh.ShopTechnology.dto.request.auth.SendOTPRequest;
import com.ndh.ShopTechnology.entities.otp.OTPEntity;
import com.ndh.ShopTechnology.exception.CustomApiException;
import com.ndh.ShopTechnology.repository.OTPRepository;
import com.ndh.ShopTechnology.repository.UserRepository;
import com.ndh.ShopTechnology.services.email.EmailService;
import com.ndh.ShopTechnology.services.otp.OTPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OTPServiceImpl implements OTPService {

    private final OTPRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    @Transactional
    @Override
    public void sendOTP(SendOTPRequest request) {
        String login = validateAndNormalizeLogin(request.getLogin());
        checkUserNotExists(login);
        checkResendCooldown(login);

        String otpCode = generateOTPCode();
        saveOrUpdateOTP(login, otpCode);
        sendOTPToEmail(login, otpCode);

        log.info("OTP sent successfully to: {}", maskLogin(login));
    }

    @Transactional
    @Override
    public boolean verifyOTP(String login, String otp) {
        String normalizedLogin = validateAndNormalizeLogin(login);
        String normalizedOtp = validateOTPFormat(otp);

        Optional<OTPEntity> otpEntityOpt = otpRepository.findByLogin(normalizedLogin);

        if (otpEntityOpt.isEmpty()) {
            log.warn("OTP not found for login: {}", maskLogin(normalizedLogin));
            return false;
        }

        OTPEntity otpEntity = otpEntityOpt.get();

        if (LocalDateTime.now().isAfter(otpEntity.getExpiredAt())) {
            log.warn("OTP expired for login: {}", maskLogin(normalizedLogin));
            return false;
        }

        // ✅ THAY ĐỔI: Cho phép verify nhiều lần (không check isVerified)
        // Vì user cần verify ở step 2, sau đó gửi lại OTP ở step 3

        if (otpEntity.getAttemptCount() >= MAX_ATTEMPTS) {
            log.warn("Maximum OTP attempts exceeded for login: {}", maskLogin(normalizedLogin));
            throw new CustomApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đã nhập sai quá nhiều lần. Vui lòng yêu cầu mã mới."
            );
        }

        if (!otpEntity.getOtpCode().equals(normalizedOtp)) {
            incrementAttemptCount(otpEntity);
            log.warn("Invalid OTP attempt for login: {}", maskLogin(normalizedLogin));
            return false;
        }

        // ✅ Đánh dấu đã verify (nhưng chưa used)
        markAsVerified(otpEntity);
        log.info("OTP verified successfully for login: {}", maskLogin(normalizedLogin));

        return true;
    }

    // ✅ THÊM: Method mới để verify OTP khi register (chỉ dùng 1 lần)
    @Transactional
    @Override
    public boolean verifyOTPForRegister(String login, String otp) {
        String normalizedLogin = validateAndNormalizeLogin(login);
        String normalizedOtp = validateOTPFormat(otp);

        Optional<OTPEntity> otpEntityOpt = otpRepository.findByLoginForUpdate(normalizedLogin);

        if (otpEntityOpt.isEmpty()) {
            log.warn("OTP not found for login: {}", maskLogin(normalizedLogin));
            return false;
        }

        OTPEntity otpEntity = otpEntityOpt.get();

        // ✅ CHECK: OTP đã được dùng để register chưa?
        if (otpEntity.isUsed()) {
            log.warn("OTP already used for registration: {}", maskLogin(normalizedLogin));
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Mã xác thực này đã được sử dụng"
            );
        }

        // Check expired
        if (LocalDateTime.now().isAfter(otpEntity.getExpiredAt())) {
            log.warn("OTP expired for login: {}", maskLogin(normalizedLogin));
            return false;
        }

        // Check OTP code
        if (!otpEntity.getOtpCode().equals(normalizedOtp)) {
            log.warn("Invalid OTP for registration: {}", maskLogin(normalizedLogin));
            return false;
        }

        // ✅ MARK AS USED - Chỉ cho phép dùng 1 lần cho register
        otpEntity.setUsed(true);
        otpEntity.setVerified(true);
        otpRepository.saveAndFlush(otpEntity); // Flush ngay để commit

        log.info("OTP verified and marked as used for registration: {}", maskLogin(normalizedLogin));

        return true;
    }

    @Override
    public void clearOTP(String login) {
        String normalizedLogin = validateAndNormalizeLogin(login);
        otpRepository.deleteByLogin(normalizedLogin);
        log.info("OTP cleared for login: {}", maskLogin(normalizedLogin));
    }

    // ==================== HELPER METHODS ====================

    private String validateAndNormalizeLogin(String login) {
        if (login == null || login.trim().isEmpty()) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Email hoặc số điện thoại không được để trống"
            );
        }

        String normalized = login.trim();

        if (!isValidEmail(normalized) && !isValidPhone(normalized)) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Email hoặc số điện thoại không hợp lệ"
            );
        }

        return normalized.toLowerCase();
    }

    private String validateOTPFormat(String otp) {
        if (otp == null || otp.trim().isEmpty()) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Mã OTP không được để trống"
            );
        }

        String normalized = otp.trim();

        if (!normalized.matches("\\d{6}")) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Mã OTP phải là 6 chữ số"
            );
        }

        return normalized;
    }

    private void checkUserNotExists(String login) {
        boolean exists = isValidEmail(login)
                ? userRepository.existsByUsername(login)
                : userRepository.existsByPhoneNumber(login);

        if (exists) {
            throw new CustomApiException(
                    HttpStatus.CONFLICT,
                    isValidEmail(login)
                            ? "Email này đã được đăng ký"
                            : "Số điện thoại này đã được đăng ký"
            );
        }
    }

    private void checkResendCooldown(String login) {
        Optional<OTPEntity> existingOtp = otpRepository.findByLogin(login);

        if (existingOtp.isPresent()) {
            LocalDateTime lastSent = existingOtp.get().getCreatedAt();
            LocalDateTime cooldownEnd = lastSent.plusSeconds(RESEND_COOLDOWN_SECONDS);

            if (LocalDateTime.now().isBefore(cooldownEnd)) {
                long secondsLeft = java.time.Duration.between(
                        LocalDateTime.now(),
                        cooldownEnd
                ).getSeconds();

                throw new CustomApiException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Vui lòng đợi " + secondsLeft + " giây trước khi gửi lại mã"
                );
            }
        }
    }

    private String generateOTPCode() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    private void saveOrUpdateOTP(String login, String otpCode) {
        Optional<OTPEntity> existingOtp = otpRepository.findByLogin(login);

        OTPEntity otpEntity;
        if (existingOtp.isPresent()) {
            otpEntity = existingOtp.get();
            otpEntity.setOtpCode(otpCode);
            otpEntity.setCreatedAt(LocalDateTime.now());
            otpEntity.setExpiredAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            otpEntity.setVerified(false);
            otpEntity.setUsed(false); // ✅ Reset used flag
            otpEntity.setAttemptCount(0);
        } else {
            otpEntity = OTPEntity.builder()
                    .login(login)
                    .otpCode(otpCode)
                    .createdAt(LocalDateTime.now())
                    .expiredAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                    .verified(false)
                    .used(false) // ✅ Set used = false
                    .attemptCount(0)
                    .build();
        }

        otpRepository.save(otpEntity);
    }

    private void sendOTPToEmail(String email, String otpCode) {
        try {
            emailService.sendOTPEmail(email, otpCode);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", maskLogin(email), e);
            throw new CustomApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể gửi email xác thực. Vui lòng thử lại sau."
            );
        }
    }

    private void incrementAttemptCount(OTPEntity otpEntity) {
        otpEntity.setAttemptCount(otpEntity.getAttemptCount() + 1);
        otpRepository.save(otpEntity);
    }

    private void markAsVerified(OTPEntity otpEntity) {
        otpEntity.setVerified(true);
        otpRepository.save(otpEntity);
    }

    private boolean isValidEmail(String input) {
        return input != null && input.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidPhone(String input) {
        return input != null && input.matches("\\d{10,15}");
    }

    private String maskLogin(String login) {
        if (login == null || login.length() < 4) {
            return "***";
        }

        if (isValidEmail(login)) {
            int atIndex = login.indexOf('@');
            String username = login.substring(0, atIndex);
            String domain = login.substring(atIndex);

            if (username.length() <= 2) {
                return "**" + domain;
            }
            return username.substring(0, 2) + "***" + domain;
        } else {
            return login.substring(0, 3) + "****" + login.substring(login.length() - 2);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredOTPs() {
        try {
            otpRepository.deleteExpiredOTPs(LocalDateTime.now());
            log.info("Expired OTPs cleaned up successfully");
        } catch (Exception e) {
            log.error("Failed to cleanup expired OTPs", e);
        }
    }
}