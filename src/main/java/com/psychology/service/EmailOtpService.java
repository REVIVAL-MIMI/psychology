package com.psychology.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.port:587}")
    private int smtpPort;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean smtpStartTlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:false}")
    private boolean smtpStartTlsRequired;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}")
    private boolean smtpSslEnable;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust:}")
    private String smtpSslTrust;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:10000}")
    private int smtpConnectionTimeoutMs;

    @Value("${spring.mail.properties.mail.smtp.timeout:10000}")
    private int smtpTimeoutMs;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:10000}")
    private int smtpWriteTimeoutMs;

    @Value("${app.email-otp.fallback-host:}")
    private String fallbackHost;

    @Value("${app.email-otp.fallback-port:0}")
    private int fallbackPort;

    @Value("${app.email-otp.fallback-auth:true}")
    private boolean fallbackAuth;

    @Value("${app.email-otp.fallback-starttls-enable:false}")
    private boolean fallbackStartTlsEnable;

    @Value("${app.email-otp.fallback-starttls-required:false}")
    private boolean fallbackStartTlsRequired;

    @Value("${app.email-otp.fallback-ssl-enable:false}")
    private boolean fallbackSslEnable;

    @Value("${app.email-otp.fallback-ssl-trust:}")
    private String fallbackSslTrust;

    public boolean sendOtpToEmail(String email, String otp) {
        if (!emailOtpEnabled) {
            return false;
        }

        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            return false;
        }

        List<String> failedAttempts = new ArrayList<>();

        JavaMailSender primaryMailSender = mailSenderProvider.getIfAvailable();
        if (primaryMailSender != null) {
            try {
                sendOtp(primaryMailSender, normalizedEmail, otp);
                log.info("OTP email sent to {} via primary SMTP {}:{}", maskEmail(normalizedEmail), smtpHost, smtpPort);
                return true;
            } catch (Exception e) {
                failedAttempts.add("primary=" + summarizeError(e));
                log.warn("Primary SMTP delivery failed for {} via {}:{}: {}",
                        maskEmail(normalizedEmail), smtpHost, smtpPort, summarizeError(e));
            }
        } else {
            failedAttempts.add("primary=JavaMailSender not configured");
            log.warn("Email OTP is enabled but JavaMailSender is not configured");
        }

        // Common Gmail fallback: some VPS providers block 587 while 465 works.
        if (shouldTryGmailSslFallback()) {
            SmtpAttempt gmailSslFallback = new SmtpAttempt(
                    "gmail-465-ssl-fallback",
                    "smtp.gmail.com",
                    465,
                    smtpAuth,
                    false,
                    false,
                    true,
                    "smtp.gmail.com"
            );
            try {
                sendOtp(createSender(gmailSslFallback), normalizedEmail, otp);
                log.info("OTP email sent to {} via fallback SMTP {}:{}",
                        maskEmail(normalizedEmail), gmailSslFallback.host(), gmailSslFallback.port());
                return true;
            } catch (Exception e) {
                failedAttempts.add(gmailSslFallback.label() + "=" + summarizeError(e));
                log.warn("Fallback SMTP delivery failed for {} via {}:{}: {}",
                        maskEmail(normalizedEmail), gmailSslFallback.host(), gmailSslFallback.port(), summarizeError(e));
            }
        }

        if (hasText(fallbackHost) && fallbackPort > 0) {
            SmtpAttempt customFallback = new SmtpAttempt(
                    "custom-fallback",
                    fallbackHost.trim(),
                    fallbackPort,
                    fallbackAuth,
                    fallbackStartTlsEnable,
                    fallbackStartTlsRequired,
                    fallbackSslEnable,
                    fallbackSslTrust
            );
            try {
                sendOtp(createSender(customFallback), normalizedEmail, otp);
                log.info("OTP email sent to {} via fallback SMTP {}:{}",
                        maskEmail(normalizedEmail), customFallback.host(), customFallback.port());
                return true;
            } catch (Exception e) {
                failedAttempts.add(customFallback.label() + "=" + summarizeError(e));
                log.warn("Fallback SMTP delivery failed for {} via {}:{}: {}",
                        maskEmail(normalizedEmail), customFallback.host(), customFallback.port(), summarizeError(e));
            }
        }

        log.warn("Failed to send OTP email to {}. Attempts: {}",
                maskEmail(normalizedEmail), String.join("; ", failedAttempts));
        return false;
    }

    private void sendOtp(JavaMailSender mailSender, String normalizedEmail, String otp) throws Exception {
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
    }

    private JavaMailSender createSender(SmtpAttempt smtpAttempt) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtpAttempt.host());
        sender.setPort(smtpAttempt.port());
        sender.setUsername(smtpUsername);
        sender.setPassword(smtpPassword);
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(smtpAttempt.auth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtpAttempt.startTlsEnable()));
        props.put("mail.smtp.starttls.required", String.valueOf(smtpAttempt.startTlsRequired()));
        props.put("mail.smtp.ssl.enable", String.valueOf(smtpAttempt.sslEnable()));
        props.put("mail.smtp.connectiontimeout", String.valueOf(smtpConnectionTimeoutMs));
        props.put("mail.smtp.timeout", String.valueOf(smtpTimeoutMs));
        props.put("mail.smtp.writetimeout", String.valueOf(smtpWriteTimeoutMs));
        if (hasText(smtpAttempt.sslTrust())) {
            props.put("mail.smtp.ssl.trust", smtpAttempt.sslTrust().trim());
        }
        return sender;
    }

    private boolean shouldTryGmailSslFallback() {
        return "smtp.gmail.com".equalsIgnoreCase(trimToEmpty(smtpHost)) && smtpPort == 587;
    }

    private String summarizeError(Exception e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getMessage();
        }
        if (message == null || message.isBlank()) {
            message = "unknown error";
        }
        if (message.length() > 180) {
            message = message.substring(0, 180) + "...";
        }
        return root.getClass().getSimpleName() + ": " + message;
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        String user = email.substring(0, at);
        String domain = email.substring(at + 1);
        String maskedUser = user.charAt(0) + "***";
        return maskedUser + "@" + domain;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record SmtpAttempt(
            String label,
            String host,
            int port,
            boolean auth,
            boolean startTlsEnable,
            boolean startTlsRequired,
            boolean sslEnable,
            String sslTrust
    ) {
    }
}
