package com.psychology.controller;

import com.psychology.model.entity.Psychologist;
import com.psychology.service.AdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // Получить список психологов на верификацию
    @GetMapping("/psychologists/pending")
    public ResponseEntity<List<Psychologist>> getPendingPsychologists() {
        List<Psychologist> pendingPsychologists = adminService.getPendingPsychologists();
        return ResponseEntity.ok(pendingPsychologists);
    }

    // Получить список всех психологов
    @GetMapping("/psychologists")
    public ResponseEntity<List<Psychologist>> getAllPsychologists() {
        List<Psychologist> psychologists = adminService.getAllPsychologists();
        return ResponseEntity.ok(psychologists);
    }

    // Верифицировать психолога
    @PostMapping("/psychologists/{psychologistId}/verify")
    public ResponseEntity<?> verifyPsychologist(@PathVariable Long psychologistId) {
        try {
            Psychologist verified = adminService.verifyPsychologist(psychologistId);
            return ResponseEntity.ok(verified);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Отклонить верификацию психолога
    @PostMapping("/psychologists/{psychologistId}/reject")
    public ResponseEntity<?> rejectPsychologist(@PathVariable Long psychologistId,
                                                @RequestBody RejectRequest request) {
        try {
            adminService.rejectPsychologist(psychologistId, request.getReason());
            return ResponseEntity.ok(new ApiResponse("Psychologist rejected successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Получить статистику
    @GetMapping("/stats")
    public ResponseEntity<AdminStats> getStats() {
        AdminStats stats = adminService.getAdminStats();
        return ResponseEntity.ok(stats);
    }

    @Data
    public static class RejectRequest {
        private String reason;
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

    @Data
    public static class AdminStats {
        private long totalPsychologists;
        private long pendingPsychologists;
        private long verifiedPsychologists;
        private long totalClients;
        private long activeSessionsToday;
    }
}