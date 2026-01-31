package com.psychology.controller;

import com.psychology.model.entity.Psychologist;
import com.psychology.service.InviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invites")
@RequiredArgsConstructor
public class InviteController {
    private final InviteService inviteService;

    @PostMapping
    public ResponseEntity<?> createInvite(@AuthenticationPrincipal Psychologist psychologist) {
        try {
            String token = inviteService.createInvite(psychologist);
            String inviteLink = "http://localhost:3000/register?invite=" + token; // Замените на ваш домен
            return ResponseEntity.ok(new InviteResponse(token, inviteLink));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Failed to create invite: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getInvites(@AuthenticationPrincipal Psychologist psychologist) {
        try {
            List<InviteDTO> invites = inviteService.getInvitesByPsychologist(psychologist.getId());
            return ResponseEntity.ok(invites);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Failed to get invites: " + e.getMessage()));
        }
    }

    @GetMapping("/validate/{token}")
    public ResponseEntity<?> validateInvite(@PathVariable String token) {
        try {
            InviteValidationResponse response = inviteService.validateInvite(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("Invalid invite: " + e.getMessage()));
        }
    }

    // DTO классы
    public static class InviteResponse {
        private String token;
        private String inviteLink;

        public InviteResponse(String token, String inviteLink) {
            this.token = token;
            this.inviteLink = inviteLink;
        }

        // Getters and setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getInviteLink() { return inviteLink; }
        public void setInviteLink(String inviteLink) { this.inviteLink = inviteLink; }
    }

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

        // Getters and setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getExpiresAt() { return expiresAt; }
        public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
        public boolean isUsed() { return used; }
        public void setUsed(boolean used) { this.used = used; }
    }

    public static class InviteValidationResponse {
        private boolean valid;
        private String psychologistName;
        private String expiresAt;

        public InviteValidationResponse(boolean valid, String psychologistName, String expiresAt) {
            this.valid = valid;
            this.psychologistName = psychologistName;
            this.expiresAt = expiresAt;
        }

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getPsychologistName() { return psychologistName; }
        public void setPsychologistName(String psychologistName) { this.psychologistName = psychologistName; }
        public String getExpiresAt() { return expiresAt; }
        public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    }

    public static class ApiResponse {
        private String message;

        public ApiResponse(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}