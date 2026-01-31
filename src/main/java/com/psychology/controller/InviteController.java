package com.psychology.controller;

import com.psychology.model.entity.Psychologist;
import com.psychology.repository.PsychologistRepository;
import com.psychology.service.InviteService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/invites")
@RequiredArgsConstructor
@Slf4j
public class InviteController {
    private final InviteService inviteService;
    private final PsychologistRepository psychologistRepository;

    @PostMapping
    @PreAuthorize("hasRole('PSYCHOLOGIST')") // Добавляем проверку роли
    public ResponseEntity<?> createInvite(@AuthenticationPrincipal Object principal) {
        log.info("=== CREATE INVITE REQUEST ===");

        try {
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse("Not authenticated"));
            }

            Psychologist psychologist;

            // Обрабатываем разные типы principal
            if (principal instanceof Psychologist) {
                psychologist = (Psychologist) principal;
                log.info("Principal is Psychologist: ID={}", psychologist.getId());
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                // Получаем phone из UserDetails
                String phone = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                log.info("Principal is UserDetails, phone: {}", phone);

                // Ищем психолога по телефону
                psychologist = psychologistRepository.findByPhone(phone)
                        .orElseThrow(() -> {
                            log.error("Psychologist not found with phone: {}", phone);
                            return new RuntimeException("Psychologist not found");
                        });
                log.info("Found psychologist from DB: ID={}", psychologist.getId());
            } else {
                log.error("Unknown principal type: {}", principal.getClass().getName());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse("Invalid user type"));
            }

            // Проверяем верификацию психолога
            if (!psychologist.isVerified()) {
                log.warn("Psychologist {} is not verified", psychologist.getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse("Psychologist account is not verified yet. Please wait for administrator verification."));
            }

            String token = inviteService.createInvite(psychologist);
            String inviteLink = "http://localhost:3000/register?invite=" + token;

            log.info("Invite created successfully for psychologist ID: {}", psychologist.getId());

            return ResponseEntity.ok(new InviteResponse(token, inviteLink));

        } catch (Exception e) {
            log.error("Error creating invite: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Failed to create invite: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<?> getInvites(@AuthenticationPrincipal Object principal) {
        try {
            Psychologist psychologist = extractPsychologist(principal);
            if (psychologist == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse("Psychologist not found"));
            }

            List<InviteDTO> invites = inviteService.getInvitesByPsychologist(psychologist.getId());
            return ResponseEntity.ok(invites);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Failed to get invites: " + e.getMessage()));
        }
    }

    @GetMapping("/validate/{token}")
    public ResponseEntity<?> validateInvite(@PathVariable String token) {
        log.info("Validating invite token: {}", token);
        try {
            InviteValidationResponse response = inviteService.validateInvite(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Invalid invite token: {}", token, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Invalid invite: " + e.getMessage()));
        }
    }
    private Psychologist extractPsychologist(Object principal) {
        if (principal instanceof Psychologist) {
            return (Psychologist) principal;
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String phone = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            return psychologistRepository.findByPhone(phone).orElse(null);
        }
        return null;
    }

    // DTO классы
    @Data
    public static class InviteResponse {
        private String token;
        private String inviteLink;

        public InviteResponse(String token, String inviteLink) {
            this.token = token;
            this.inviteLink = inviteLink;
        }
    }

    @Data
    public static class InviteDTO {
        private String token;
        private String createdAt;
        private String expiresAt;
        private boolean used;

        public InviteDTO(String token, String createdAt, String expiresAt, boolean used) {
            this.token = token;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.used = used;
        }
    }

    @Data
    public static class InviteValidationResponse {
        private boolean valid;
        private String psychologistName;
        private String expiresAt;

        public InviteValidationResponse(boolean valid, String psychologistName, String expiresAt) {
            this.valid = valid;
            this.psychologistName = psychologistName;
            this.expiresAt = expiresAt;
        }
    }

    @Data
    public static class ApiResponse {
        private String message;
        private LocalDateTime timestamp;

        public ApiResponse(String message) {
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }
    }
}