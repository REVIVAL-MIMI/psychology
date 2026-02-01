package com.psychology.ratelimiting;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    // Лимиты запросов
    private static final int AUTH_LIMIT = 10; // 10 запросов в минуту на авторизацию
    private static final int OTP_LIMIT = 3; // 3 запроса OTP в минуту
    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        String path = request.getRequestURI();
        String key;
        int limit;

        // Определяем лимит в зависимости от пути
        if (path.contains("/auth/send-otp")) {
            key = "rate_limit:otp:" + ip;
            limit = OTP_LIMIT;
        } else if (path.contains("/auth/")) {
            key = "rate_limit:auth:" + ip;
            limit = AUTH_LIMIT;
        } else {
            return true; // Для других путей не применяем rate limiting
        }

        // Проверяем количество запросов
        Long current = redisTemplate.opsForValue().increment(key);
        if (current == null) {
            // Ошибка Redis, пропускаем
            return true;
        }

        if (current == 1) {
            // Первый запрос, устанавливаем TTL
            redisTemplate.expire(key, WINDOW.getSeconds(), TimeUnit.SECONDS);
        }

        if (current > limit) {
            log.warn("Rate limit exceeded for IP: {}, path: {}", ip, path);
            response.setStatus(429); // Too Many Requests
            response.getWriter().write("Rate limit exceeded. Please try again later.");
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}