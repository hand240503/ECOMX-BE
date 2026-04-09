package com.ndh.ShopTechnology.services.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email OTP với HTML template (Async)
     */
    @Async("mailTaskExecutor")
    public CompletableFuture<Void> sendOTPEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(Objects.requireNonNull(toEmail));
            helper.setSubject("Mã xác thực đăng ký tài khoản ECOMX");

            String htmlContent = buildOTPEmailContent(otpCode);
            helper.setText(Objects.requireNonNull(htmlContent), true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            return CompletableFuture.failedFuture(new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.", e));
        }
    }

    /**
     * Gửi email đơn giản (không dùng HTML) - Async
     */
    @Async("mailTaskExecutor")
    public CompletableFuture<Void> sendSimpleOTPEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Mã xác thực đăng ký tài khoản ECOMX");
            message.setText(
                    "Mã xác thực của bạn là: " + otpCode + "\n\n" +
                            "Mã này có hiệu lực trong 5 phút.\n" +
                            "Vui lòng không chia sẻ mã này với bất kỳ ai.\n\n" +
                            "Trân trọng,\n" +
                            "Đội ngũ ECOMX"
            );

            mailSender.send(message);
            log.info("Simple OTP email sent successfully to: {}", toEmail);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send simple OTP email to: {}", toEmail, e);
            return CompletableFuture.failedFuture(new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.", e));
        }
    }

    @Async("mailTaskExecutor")
    public CompletableFuture<Void> sendPasswordResetLinkEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(Objects.requireNonNull(toEmail));
            helper.setSubject("Liên kết đặt lại mật khẩu ECOMX");

            String htmlContent = "<!DOCTYPE html>" +
                    "<html><body style='font-family:Arial,sans-serif;color:#333;'>" +
                    "<h2>Đặt lại mật khẩu</h2>" +
                    "<p>Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản ECOMX.</p>" +
                    "<p>Nhấn vào liên kết bên dưới để tạo mật khẩu mới:</p>" +
                    "<p><a href='" + resetLink + "'>" + resetLink + "</a></p>" +
                    "<p>Liên kết có hiệu lực trong 15 phút và chỉ dùng được 1 lần.</p>" +
                    "<p>Nếu bạn không yêu cầu, vui lòng bỏ qua email này.</p>" +
                    "</body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Password reset link email sent successfully to: {}", toEmail);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send password reset link email to: {}", toEmail, e);
            return CompletableFuture.failedFuture(new RuntimeException("Không thể gửi email đặt lại mật khẩu. Vui lòng thử lại sau.", e));
        }
    }

    private String buildOTPEmailContent(String otpCode) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "        .header { background: linear-gradient(135deg, #EF4444 0%, #DC2626 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                "        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                "        .otp-box { background: white; border: 2px dashed #EF4444; padding: 20px; text-align: center; margin: 20px 0; border-radius: 8px; }" +
                "        .otp-code { font-size: 36px; font-weight: bold; color: #EF4444; letter-spacing: 8px; }" +
                "        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }" +
                "        .warning { background: #FEF3C7; border-left: 4px solid #F59E0B; padding: 12px; margin: 20px 0; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>ECOMX</h1>" +
                "            <p>Xác thực tài khoản của bạn</p>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <h2>Xin chào!</h2>" +
                "            <p>Cảm ơn bạn đã đăng ký tài khoản tại ECOMX. Để hoàn tất quá trình đăng ký, vui lòng sử dụng mã xác thực dưới đây:</p>" +
                "            <div class='otp-box'>" +
                "                <div class='otp-code'>" + otpCode + "</div>" +
                "            </div>" +
                "            <p><strong>Lưu ý:</strong></p>" +
                "            <ul>" +
                "                <li>Mã xác thực có hiệu lực trong <strong>5 phút</strong></li>" +
                "                <li>Vui lòng không chia sẻ mã này với bất kỳ ai</li>" +
                "                <li>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email</li>" +
                "            </ul>" +
                "            <div class='warning'>" +
                "                <strong>Cảnh báo bảo mật:</strong> ECOMX sẽ không bao giờ yêu cầu mã xác thực của bạn qua điện thoại hoặc email." +
                "            </div>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>© 2024 ECOMX. All rights reserved.</p>" +
                "            <p>Email này được gửi tự động, vui lòng không trả lời.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
}