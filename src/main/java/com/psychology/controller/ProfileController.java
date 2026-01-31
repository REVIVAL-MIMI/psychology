package com.psychology.controller;

import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.Client;
import com.psychology.service.ProfileService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // Получить профиль текущего пользователя
    @GetMapping
    public ResponseEntity<?> getCurrentProfile(@AuthenticationPrincipal Object user) {
        if (user instanceof Psychologist) {
            return ResponseEntity.ok(profileService.getPsychologistProfile((Psychologist) user));
        } else if (user instanceof Client) {
            return ResponseEntity.ok(profileService.getClientProfile((Client) user));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse("User type not recognized"));
    }

    // Обновить профиль психолога
    @PutMapping("/psychologist")
    public ResponseEntity<?> updatePsychologistProfile(
            @AuthenticationPrincipal Psychologist psychologist,
            @RequestBody PsychologistProfileUpdateRequest request) {
        try {
            Psychologist updated = profileService.updatePsychologistProfile(psychologist, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Обновить профиль клиента
    @PutMapping("/client")
    public ResponseEntity<?> updateClientProfile(
            @AuthenticationPrincipal Client client,
            @RequestBody ClientProfileUpdateRequest request) {
        try {
            Client updated = profileService.updateClientProfile(client, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // DTO для обновления профиля психолога
    @Data
    public static class PsychologistProfileUpdateRequest {
        private String fullName;
        private String email;
        private String education;
        private String specialization;
        private String description;
        private String photoUrl;
    }

    // DTO для обновления профиля клиента
    @Data
    public static class ClientProfileUpdateRequest {
        private String fullName;
        private Integer age;
        private String photoUrl;
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