package com.psychology.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @JsonAlias({"email", "phone"})
    @NotBlank
    @Email(message = "Invalid email format")
    private String phone;

    @NotBlank
    private String otp;
}
