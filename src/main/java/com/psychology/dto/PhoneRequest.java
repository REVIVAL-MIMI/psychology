package com.psychology.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PhoneRequest {
    @JsonAlias({"email", "phone"})
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String phone;
}
