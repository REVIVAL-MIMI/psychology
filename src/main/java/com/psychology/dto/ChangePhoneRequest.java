package com.psychology.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePhoneRequest {
    @NotBlank
    private String phone;

    @NotBlank
    private String otp;
}
