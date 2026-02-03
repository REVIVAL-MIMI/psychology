package com.psychology.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

public class AuthDTO {

    @Data
    public static class PhoneRequest {
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        private String phone;
    }

    @Data
    public static class VerifyOtpRequest {
        @NotBlank
        private String phone;

        @NotBlank
        private String otp;
    }

    @Data
    public static class AuthResponse {
        private String accessToken;
        private Long userId;
        private String userRole;
        private String fullName;
        private String phone;
        private Boolean verified;
    }

    @Data
    public static class PsychologistRegisterRequest {
        @NotBlank
        private String phone;

        @NotBlank
        private String otp;

        @NotBlank
        private String fullName;

        @NotBlank
        @Email
        private String email;

        private String education;
        private String specialization;
        private String description;
    }

    @Data
    public static class ClientRegisterRequest {
        @NotBlank
        private String phone;

        @NotBlank
        private String otp;

        @NotBlank
        private String fullName;

        @NotNull
        private Integer age;
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank
        private String refreshToken;
    }

    @Data
    public static class AdminLoginRequest {
        @NotBlank
        private String login;

        @NotBlank
        private String password;
    }

    @Data
    public static class AdminLoginResponse {
        private String accessToken;
        private String userRole;
    }

    @Data
    public static class ApiResponse {
        private String message;
        private Object data;

        public ApiResponse(String message) {
            this.message = message;
        }

        public ApiResponse(String message, Object data) {
            this.message = message;
            this.data = data;
        }
    }
}
