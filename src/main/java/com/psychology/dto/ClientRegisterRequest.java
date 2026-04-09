package com.psychology.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class ClientRegisterRequest {
    @NotBlank
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
