package com.psychology.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailOtpService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.email-otp.enabled:false}")
    private boolean emailOtpEnabled;

    @Value("${app.email-otp.from:}")
    private String fromAddress;

    @Value("${app.email-otp.subject:Код входа}")
    private String subject;

    public boolean sendOtpToEmail(String email, String otp) {
        if (!emailOtpEnabled) {
            return false;
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            return false;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Email OTP is enabled but JavaMailSender is not configured");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(normalizedEmail);
            if (hasText(fromAddress)) {
                helper.setFrom(fromAddress.trim());
            }
            helper.setSubject(subject);
            helper.setText("""
                    Ваш код подтверждения: %s

                    Код действует 5 минут.
                    Если это были не вы, просто проигнорируйте письмо.
                    """.formatted(otp).trim(), false);

            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send OTP email to {}: {}", normalizedEmail, e.getMessage());
            return false;
        }
    }

    private String normalizeEmail(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
