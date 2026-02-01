package com.psychology.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientDTO {
    private Long id;
    private String phone;
    private String fullName;
    private Integer age;
    private String photoUrl;
    private LocalDateTime linkedAt;
    private PsychologistInfoDTO psychologist; // Только ID и имя
}
