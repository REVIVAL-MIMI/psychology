package com.psychology.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientDTO {
    private Long id;
    private String phone;
    private String fullName;
    private Integer age;
    private String companyName;
    private String workEmail;
    private String department;
    private String position;
    private String employeeCode;
    private LocalDateTime linkedAt;
    private PsychologistInfoDTO psychologist; // Только ID и имя
}
