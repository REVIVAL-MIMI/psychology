package com.psychology.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class ClientRegisterRequest {
    @JsonAlias({"email", "phone"})
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
