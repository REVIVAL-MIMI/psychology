package com.psychology.controller;

import com.psychology.dto.AuthDTO.AdminLoginRequest;
import com.psychology.dto.AuthDTO.AdminLoginResponse;
import jakarta.servlet.http.HttpServletResponse;
import com.psychology.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${admin.login:admin}")
    private String adminLogin;

    @Value("${admin.password:admin}")
    private String adminPassword;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String REFRESH_PREFIX = "refresh:";

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest request,
                                   HttpServletResponse httpServletResponse) {
        if (!adminLogin.equals(request.getLogin()) || !adminPassword.equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse("Invalid admin credentials"));
        }

        UserDetails adminUser = buildAdminUser();

        String accessToken = jwtTokenProvider.generateAccessToken(adminUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(adminUser);

        // Сохраняем refresh token в Redis
        stringRedisTemplate.opsForValue().set(
                REFRESH_PREFIX + adminLogin,
                refreshToken,
                refreshExpirationMs,
                TimeUnit.MILLISECONDS
        );

        setRefreshTokenCookie(httpServletResponse, refreshToken);

        AdminLoginResponse response = new AdminLoginResponse();
        response.setAccessToken(accessToken);
        response.setUserRole("ROLE_ADMIN");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(value = "adminRefreshToken", required = false) String refreshToken,
            HttpServletResponse httpServletResponse) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse("Refresh token is missing"));
        }

        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + refreshToken))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse("Token is blacklisted"));
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse("Invalid refresh token"));
        }

        String username = jwtTokenProvider.extractUsername(refreshToken);
        if (!adminLogin.equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse("Invalid admin token"));
        }

        String storedRefreshToken = stringRedisTemplate.opsForValue().get(REFRESH_PREFIX + adminLogin);
        if (!refreshToken.equals(storedRefreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse("Refresh token mismatch"));
        }

        // Блеклистим старый refresh token
        stringRedisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + refreshToken,
                "blacklisted",
                refreshExpirationMs,
                TimeUnit.MILLISECONDS
        );

        UserDetails adminUser = buildAdminUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(adminUser);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(adminUser);

        stringRedisTemplate.opsForValue().set(
                REFRESH_PREFIX + adminLogin,
                newRefreshToken,
                refreshExpirationMs,
                TimeUnit.MILLISECONDS
        );

        setRefreshTokenCookie(httpServletResponse, newRefreshToken);

        AdminLoginResponse response = new AdminLoginResponse();
        response.setAccessToken(newAccessToken);
        response.setUserRole("ROLE_ADMIN");

        return ResponseEntity.ok(response);
    }

    private UserDetails buildAdminUser() {
        return org.springframework.security.core.userdetails.User
                .withUsername(adminLogin)
                .password("")
                .roles("ADMIN")
                .build();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("adminRefreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/admin/refresh")
                .sameSite("Lax")
                .maxAge(Duration.ofMillis(refreshExpirationMs))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @lombok.Data
    public static class ApiResponse {
        private String message;

        public ApiResponse(String message) {
            this.message = message;
        }
    }
}
