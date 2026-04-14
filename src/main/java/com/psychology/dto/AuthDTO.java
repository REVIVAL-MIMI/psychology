package com.psychology.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class AuthDTO {

    @Data
    public static class PhoneRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String phone;
    }

    @Data
    public static class VerifyOtpRequest {
        @NotBlank
        @Email(message = "Invalid email format")
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
        @Email(message = "Invalid email format")
        private String phone;

        @NotBlank
        private String otp;

        @NotBlank
        private String fullName;

        @NotBlank
        @Email
        private String email;

        private String organizationName;
        private String serviceFormat;
        private String education;
        private String specialization;
        private String description;
    }

    @Data
    public static class ClientRegisterRequest {
        @NotBlank
        @Email(message = "Invalid email format")
        private String phone;

        @NotBlank
        private String otp;

        @NotBlank
        private String fullName;

        private Integer age;
        private String companyName;
        @Email
        private String workEmail;
        private String department;
        private String position;
        private String employeeCode;
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
