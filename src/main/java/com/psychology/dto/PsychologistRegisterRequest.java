package com.psychology.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PsychologistRegisterRequest {
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
    private String photoUrl;
}