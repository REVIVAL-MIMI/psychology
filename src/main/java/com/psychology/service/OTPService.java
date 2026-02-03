package com.psychology.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class OTPService {
    private final StringRedisTemplate stringRedisTemplate;

    private static final String OTP_PREFIX = "otp:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp_attempts:";
    private static final String BLOCKED_PREFIX = "blocked:";
    private static final String SEND_TIMEOUT_PREFIX = "otp_timeout:";

    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final int BLOCK_DURATION_MINUTES = 60;
    private static final int SEND_TIMEOUT_SECONDS = 60; // 1 minute

    public String generateOTP(String phone) {
        // Проверка блокировки
        if (isBlocked(phone)) {
            throw new RuntimeException("Phone is temporarily blocked");
        }

        // Проверка таймаута отправки
        String timeoutKey = SEND_TIMEOUT_PREFIX + phone;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(timeoutKey))) {
            throw new RuntimeException("Please wait before requesting new OTP");
        }

        // Генерация OTP
        String otp = RandomStringUtils.randomNumeric(OTP_LENGTH);
        String key = OTP_PREFIX + phone;

        // Сохранение в Redis
        stringRedisTemplate.opsForValue().set(
                key,
                otp,
                OTP_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        // Сохраняем OTP для админки (последний по номеру)
        String adminKey = "otp_admin:" + phone;
        stringRedisTemplate.opsForValue().set(
                adminKey,
                otp,
                OTP_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        // Установка таймаута на повторную отправку
        stringRedisTemplate.opsForValue().set(
                timeoutKey,
                "1",
                SEND_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        );

        // Логирование OTP (в продакшене будет отправка SMS)
        log.info("OTP for {}: {}", phone, otp);

        // Добавляем в список последних OTP для админки
        String recentKey = "otp_admin_recent";
        String payload = phone + "|" + otp + "|" + System.currentTimeMillis();
        stringRedisTemplate.opsForList().leftPush(recentKey, payload);
        stringRedisTemplate.opsForList().trim(recentKey, 0, 49); // храним последние 50

        return otp;
    }

    public boolean verifyOTP(String phone, String otp) {
        String key = OTP_PREFIX + phone;
        String storedOtp = stringRedisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            return false;
        }

        boolean verified = storedOtp.equals(otp);

        if (verified) {
            // Удаляем OTP после успешной проверки
            stringRedisTemplate.delete(key);
            resetAttempts(phone);
        } else {
            incrementAttempts(phone);
        }

        return verified;
    }

    private void incrementAttempts(String phone) {
        String key = OTP_ATTEMPTS_PREFIX + phone;
        Long attempts = stringRedisTemplate.opsForValue().increment(key);

        if (attempts == 1) {
            stringRedisTemplate.expire(key, 1, TimeUnit.HOURS);
        }

        if (attempts >= MAX_ATTEMPTS) {
            blockPhone(phone);
            throw new RuntimeException("Too many OTP attempts");
        }
    }

    private void resetAttempts(String phone) {
        String key = OTP_ATTEMPTS_PREFIX + phone;
        stringRedisTemplate.delete(key);
    }

    private boolean isBlocked(String phone) {
        String key = BLOCKED_PREFIX + phone;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    private void blockPhone(String phone) {
        String key = BLOCKED_PREFIX + phone;
        stringRedisTemplate.opsForValue().set(key, "blocked", BLOCK_DURATION_MINUTES, TimeUnit.MINUTES);
    }
}
