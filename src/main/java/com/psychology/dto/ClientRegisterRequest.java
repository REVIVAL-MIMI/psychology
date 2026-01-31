package com.psychology.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClientRegisterRequest {
    @NotBlank
    private String phone;

    @NotBlank
    private String otp;

    @NotBlank
    private String fullName;

    @NotNull
    private Integer age;

    private String photoUrl;
}