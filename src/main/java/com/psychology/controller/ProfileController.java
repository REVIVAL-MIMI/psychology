package com.psychology.controller;

import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.Client;
import com.psychology.dto.PhoneRequest;
import com.psychology.dto.ChangePhoneRequest;
import com.psychology.service.ProfileService;
import com.psychology.service.AuthService;
import com.psychology.service.OTPService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final OTPService otpService;
    private final AuthService authService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    // Получить профиль текущего пользователя
    @GetMapping
    public ResponseEntity<?> getCurrentProfile(@AuthenticationPrincipal Object user) {
        if (user instanceof Psychologist) {
            return ResponseEntity.ok(profileService.getPsychologistProfile((Psychologist) user));
        } else if (user instanceof Client) {
            return ResponseEntity.ok(profileService.getClientProfile((Client) user));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse("User type not recognized"));
    }

    // Обновить профиль психолога
    @PutMapping("/psychologist")
    public ResponseEntity<?> updatePsychologistProfile(
            @AuthenticationPrincipal Psychologist psychologist,
            @RequestBody PsychologistProfileUpdateRequest request) {
        try {
            Psychologist updated = profileService.updatePsychologistProfile(psychologist, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Обновить профиль клиента
    @PutMapping("/client")
    public ResponseEntity<?> updateClientProfile(
            @AuthenticationPrincipal Client client,
            @RequestBody ClientProfileUpdateRequest request) {
        try {
            Client updated = profileService.updateClientProfile(client, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @GetMapping("/verification-status")
    public ResponseEntity<?> getVerificationStatus(@AuthenticationPrincipal Object user) {
        if (user instanceof Psychologist psychologist) {
            return ResponseEntity.ok(new VerificationStatusResponse(psychologist.isVerified()));
        }
        return ResponseEntity.ok(new VerificationStatusResponse(true));
    }

    @PostMapping("/phone/send-otp")
    public ResponseEntity<?> sendPhoneOtp(@AuthenticationPrincipal Object user,
                                          @Valid @RequestBody PhoneRequest request) {
        try {
            otpService.generateOTP(request.getPhone());
            return ResponseEntity.ok(new ApiResponse("OTP sent successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @PostMapping("/phone/confirm")
    public ResponseEntity<?> confirmPhoneChange(@AuthenticationPrincipal Object user,
                                                @Valid @RequestBody ChangePhoneRequest request,
                                                jakarta.servlet.http.HttpServletResponse response) {
        if (!(user instanceof com.psychology.model.entity.User currentUser)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse("User type not recognized"));
        }

        try {
            AuthService.AuthResult result = authService.changePhone(currentUser, request.getPhone(), request.getOtp());
            setRefreshTokenCookie(response, result.refreshToken());
            return ResponseEntity.ok(result.response());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    private void setRefreshTokenCookie(jakarta.servlet.http.HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth/refresh")
                .sameSite("Lax")
                .maxAge(Duration.ofMillis(refreshExpirationMs))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // DTO для обновления профиля психолога
    @Data
    public static class PsychologistProfileUpdateRequest {
        private String fullName;
        private String email;
        private String education;
        private String specialization;
        private String description;
    }

    // DTO для обновления профиля клиента
    @Data
    public static class ClientProfileUpdateRequest {
        private String fullName;
        private Integer age;
    }

    @Data
    public static class ApiResponse {
        private String message;
        private LocalDateTime timestamp;

        public ApiResponse(String message) {
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }
    }

    @Data
    public static class VerificationStatusResponse {
        private boolean verified;

        public VerificationStatusResponse(boolean verified) {
            this.verified = verified;
        }
    }
}
