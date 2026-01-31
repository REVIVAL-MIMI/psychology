package com.psychology.controller;

import com.psychology.dto.AuthDTO.*;
import com.psychology.service.AuthService;
import com.psychology.service.OTPService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final OTPService otpService;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse> sendOTP(@Valid @RequestBody PhoneRequest request) {
        try {
            String otp = otpService.generateOTP(request.getPhone());
            return ResponseEntity.ok(new ApiResponse("OTP sent successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTP(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            AuthResponse response = authService.verifyOTPAndAuthenticate(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @PostMapping("/psychologist/register")
    public ResponseEntity<?> registerPsychologist(
            @Valid @RequestBody PsychologistRegisterRequest request) {
        try {
            // Проверяем OTP перед регистрацией
            if (!otpService.verifyOTP(request.getPhone(), request.getOtp())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse("Invalid OTP"));
            }

            AuthResponse response = authService.registerPsychologist(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

            AuthResponse response = authService.registerClient(request, inviteToken);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse("Refresh token is missing"));
        }

        try {
            AuthResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
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

        // Удаляем refresh token cookie
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok(new ApiResponse("Logged out successfully"));
    }
}