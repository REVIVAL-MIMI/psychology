package com.psychology.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PsychologistDTO {
    private Long id;
    private String phone;
    private String fullName;
    private String email;
    private String education;
    private String specialization;
    private String description;
    private String photoUrl;
    private boolean verified;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
}