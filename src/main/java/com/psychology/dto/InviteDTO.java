package com.psychology.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InviteDTO {
    private String token;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean used;
    private String psychologistName;
}