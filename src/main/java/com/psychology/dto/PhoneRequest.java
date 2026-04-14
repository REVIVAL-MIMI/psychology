package com.psychology.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String phone;
}
