package com.psychology.controller;

import com.psychology.dto.AuthDTO.*;
import com.psychology.service.AuthService;
import com.psychology.service.OTPService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final OTPService otpService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse> sendOTP(@Valid @RequestBody PhoneRequest request) {
        try {
            otpService.generateOTP(request.getPhone());
            return ResponseEntity.ok(new ApiResponse("OTP sent successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTP(@Valid @RequestBody VerifyOtpRequest request,
                                       HttpServletResponse httpServletResponse) {
        try {
            AuthService.AuthResult result = authService.verifyOTPAndAuthenticate(request);
            setRefreshTokenCookie(httpServletResponse, result.refreshToken());
            return ResponseEntity.ok(result.response());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @PostMapping("/psychologist/register")
    public ResponseEntity<?> registerPsychologist(
            @Valid @RequestBody PsychologistRegisterRequest request,
            HttpServletResponse httpServletResponse) {
        try {
            // Проверяем OTP перед регистрацией
            if (!otpService.verifyOTP(request.getPhone(), request.getOtp())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse("Invalid OTP"));
            }

            AuthService.AuthResult result = authService.registerPsychologist(request);
            setRefreshTokenCookie(httpServletResponse, result.refreshToken());
            return ResponseEntity.status(HttpStatus.CREATED).body(result.response());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @PostMapping("/client/register")
    public ResponseEntity<?> registerClient(
            @Valid @RequestBody ClientRegisterRequest request,
            @RequestParam("inviteToken") String inviteToken,
            HttpServletResponse httpServletResponse) {
        try {
            // Проверяем OTP перед регистрацией
            if (!otpService.verifyOTP(request.getPhone(), request.getOtp())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse("Invalid OTP"));
            }

            AuthService.AuthResult result = authService.registerClient(request, inviteToken);
            setRefreshTokenCookie(httpServletResponse, result.refreshToken());
            return ResponseEntity.status(HttpStatus.CREATED).body(result.response());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpServletResponse) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse("Refresh token is missing"));
        }

        try {
            AuthService.AuthResult result = authService.refreshToken(refreshToken);
            setRefreshTokenCookie(httpServletResponse, result.refreshToken());
            return ResponseEntity.ok(result.response());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            authService.logout(accessToken, refreshToken);
        }

        clearRefreshTokenCookie(response);

        return ResponseEntity.ok(new ApiResponse("Logged out successfully"));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth/refresh")
                .sameSite("Lax")
                .maxAge(Duration.ofMillis(refreshExpirationMs))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth/refresh")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
