package com.psychology.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    @Email(message = "Invalid email format")
    private String phone;

    @NotBlank
    private String otp;
}
