package com.ndh.ShopTechnology.services.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email OTP với HTML template (Async)
     */
    @Async
    public CompletableFuture<Void> sendOTPEmail(String toEmail, String otpCode) {
        return CompletableFuture.runAsync(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(toEmail);
                helper.setSubject("Mã xác thực đăng ký tài khoản ECOMX");

                String htmlContent = buildOTPEmailContent(otpCode);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("OTP email sent successfully to: {}", toEmail);

            } catch (Exception e) {
                log.error("Failed to send OTP email to: {}", toEmail, e);
                throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.");
            }
        });
    }

    /**
     * Gửi email đơn giản (không dùng HTML) - Async
     */
    @Async
    public CompletableFuture<Void> sendSimpleOTPEmail(String toEmail, String otpCode) {
        return CompletableFuture.runAsync(() -> {
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

            } catch (Exception e) {
                log.error("Failed to send simple OTP email to: {}", toEmail, e);
                throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.");
            }
        });
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
                "                <strong>⚠️ Cảnh báo bảo mật:</strong> ECOMX sẽ không bao giờ yêu cầu mã xác thực của bạn qua điện thoại hoặc email." +
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