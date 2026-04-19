package com.psychology.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PsychologistRegisterRequest {
    @JsonAlias({"email", "phone"})
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
