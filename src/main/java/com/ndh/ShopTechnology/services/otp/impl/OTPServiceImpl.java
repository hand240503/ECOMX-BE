package com.ndh.ShopTechnology.services.otp.impl;

import com.ndh.ShopTechnology.dto.request.auth.SendOTPRequest;
import com.ndh.ShopTechnology.entities.otp.OTPEntity;
import com.ndh.ShopTechnology.entities.otp.OTPPurpose;
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
import java.util.Objects;
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
        checkResendCooldown(login, OTPPurpose.REGISTER);

        String otpCode = generateOTPCode();
        saveOrUpdateOTP(login, otpCode, OTPPurpose.REGISTER);

        if (isValidEmail(login)) {
            sendOTPToEmail(login, otpCode);
        } else {
            // TODO: send OTP via SMS
            log.info("OTP generated for phone (SMS not implemented yet): {}", maskLogin(login));
        }
    }

    @Transactional
    @Override
    public boolean verifyOTP(String login, String otp) {
        return verifyOTPByPurpose(login, otp, OTPPurpose.REGISTER, false);
    }

    @Transactional
    @Override
    public boolean verifyOTPForRegister(String login, String otp) {
        return verifyOTPByPurpose(login, otp, OTPPurpose.REGISTER, true);
    }

    @Transactional
    @Override
    public void sendForgotPasswordOTP(String login, String destinationEmail) {
        String normalizedLogin = validateAndNormalizeLogin(login);
        if (!isValidEmail(destinationEmail)) {
            throw new CustomApiException(HttpStatus.BAD_REQUEST, "Email nhận OTP không hợp lệ");
        }
        checkResendCooldown(normalizedLogin, OTPPurpose.FORGOT_PASSWORD);
        String otpCode = generateOTPCode();
        saveOrUpdateOTP(normalizedLogin, otpCode, OTPPurpose.FORGOT_PASSWORD);
        sendOTPToEmail(destinationEmail.toLowerCase(), otpCode);
    }

    @Transactional
    @Override
    public boolean verifyOTPForForgotPassword(String login, String otp) {
        return verifyOTPByPurpose(login, otp, OTPPurpose.FORGOT_PASSWORD, true);
    }

    @Override
    public void clearOTP(String login) {
        String normalizedLogin = validateAndNormalizeLogin(login);
        otpRepository.deleteByLoginAndPurpose(normalizedLogin, OTPPurpose.REGISTER);
        log.info("OTP cleared for login: {}", maskLogin(normalizedLogin));
    }

    @Override
    public void clearForgotPasswordOTP(String login) {
        String normalizedLogin = validateAndNormalizeLogin(login);
        otpRepository.deleteByLoginAndPurpose(normalizedLogin, OTPPurpose.FORGOT_PASSWORD);
        log.info("Forgot-password OTP cleared for login: {}", maskLogin(normalizedLogin));
    }

    // ==================== HELPER METHODS ====================

    private String validateAndNormalizeLogin(String login) {
        if (login == null || login.trim().isEmpty()) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Email hoặc số điện thoại không được để trống");
        }

        String normalized = login.trim();

        if (!isValidEmail(normalized) && !isValidPhone(normalized)) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Email hoặc số điện thoại không hợp lệ");
        }

        return normalized.toLowerCase();
    }

    private String validateOTPFormat(String otp) {
        if (otp == null || otp.trim().isEmpty()) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Mã OTP không được để trống");
        }

        String normalized = otp.trim();

        if (!normalized.matches("\\d{6}")) {
            throw new CustomApiException(
                    HttpStatus.BAD_REQUEST,
                    "Mã OTP phải là 6 chữ số");
        }

        return normalized;
    }

    private void checkUserNotExists(String login) {
        boolean exists = isValidEmail(login)
                ? userRepository.existsByEmail(login)
                : userRepository.existsByPhoneNumber(login);

        if (exists) {
            throw new CustomApiException(
                    HttpStatus.CONFLICT,
                    isValidEmail(login)
                            ? "Email này đã được đăng ký"
                            : "Số điện thoại này đã được đăng ký");
        }
    }

    private void checkResendCooldown(String login, OTPPurpose purpose) {
        Optional<OTPEntity> existingOtp = otpRepository.findByLoginAndPurpose(login, purpose);

        if (existingOtp.isPresent()) {
            LocalDateTime lastSent = existingOtp.get().getCreatedAt();
            LocalDateTime cooldownEnd = lastSent.plusSeconds(RESEND_COOLDOWN_SECONDS);

            if (LocalDateTime.now().isBefore(cooldownEnd)) {
                long secondsLeft = java.time.Duration.between(
                        LocalDateTime.now(),
                        cooldownEnd).getSeconds();

                throw new CustomApiException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Vui lòng đợi " + secondsLeft + " giây trước khi gửi lại mã");
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

    private void saveOrUpdateOTP(String login, String otpCode, OTPPurpose purpose) {
        Optional<OTPEntity> existingOtp = otpRepository.findByLoginAndPurpose(login, purpose);

        OTPEntity otpEntity;
        if (existingOtp.isPresent()) {
            otpEntity = existingOtp.get();
            otpEntity.setOtpCode(otpCode);
            otpEntity.setPurpose(purpose);
            otpEntity.setCreatedAt(LocalDateTime.now());
            otpEntity.setExpiredAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            otpEntity.setVerified(false);
            otpEntity.setUsed(false);
            otpEntity.setAttemptCount(0);
        } else {
            otpEntity = OTPEntity.builder()
                    .login(login)
                    .purpose(purpose)
                    .otpCode(otpCode)
                    .createdAt(LocalDateTime.now())
                    .expiredAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                    .verified(false)
                    .used(false)
                    .attemptCount(0)
                    .build();
        }

        otpRepository.save(Objects.requireNonNull(otpEntity));
    }

    private boolean verifyOTPByPurpose(String login, String otp, OTPPurpose purpose, boolean markUsedWhenSuccess) {
        String normalizedLogin = validateAndNormalizeLogin(login);
        String normalizedOtp = validateOTPFormat(otp);

        Optional<OTPEntity> otpEntityOpt = otpRepository.findByLoginAndPurposeForUpdate(normalizedLogin, purpose);
        if (otpEntityOpt.isEmpty()) {
            log.warn("OTP not found for login: {}, purpose={}", maskLogin(normalizedLogin), purpose);
            return false;
        }

        OTPEntity otpEntity = otpEntityOpt.get();
        if (otpEntity.isUsed()) {
            log.warn("OTP already used: login={}, purpose={}", maskLogin(normalizedLogin), purpose);
            return false;
        }
        if (LocalDateTime.now().isAfter(otpEntity.getExpiredAt())) {
            log.warn("OTP expired for login: {}, purpose={}", maskLogin(normalizedLogin), purpose);
            return false;
        }
        if (otpEntity.getAttemptCount() >= MAX_ATTEMPTS) {
            log.warn("Maximum OTP attempts exceeded for login: {}, purpose={}", maskLogin(normalizedLogin), purpose);
            throw new CustomApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đã nhập sai quá nhiều lần. Vui lòng yêu cầu mã mới.");
        }
        if (!otpEntity.getOtpCode().equals(normalizedOtp)) {
            incrementAttemptCount(otpEntity);
            log.warn("Invalid OTP attempt for login: {}, purpose={}", maskLogin(normalizedLogin), purpose);
            return false;
        }

        otpEntity.setVerified(true);
        if (markUsedWhenSuccess) {
            otpEntity.setUsed(true);
        }
        otpRepository.saveAndFlush(otpEntity);
        log.info("OTP verified successfully for login: {}, purpose={}", maskLogin(normalizedLogin), purpose);
        return true;
    }

    private void sendOTPToEmail(String email, String otpCode) {
        try {
            emailService.sendOTPEmail(email, otpCode);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", maskLogin(email), e);
            throw new CustomApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể gửi email xác thực. Vui lòng thử lại sau.");
        }
    }

    private void incrementAttemptCount(OTPEntity otpEntity) {
        otpEntity.setAttemptCount(otpEntity.getAttemptCount() + 1);
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