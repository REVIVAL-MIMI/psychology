package com.psychology.controller;

import com.psychology.dto.ClientDTO;
import com.psychology.model.entity.Client;
import com.psychology.model.entity.Psychologist;
import com.psychology.service.ClientManagementService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientManagementController {

    private final ClientManagementService clientManagementService;

    @GetMapping
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<List<ClientDTO>> getAllClients(@AuthenticationPrincipal Psychologist psychologist) {
        List<Client> clients = clientManagementService.getAllClients(psychologist);
        List<ClientDTO> dtos = clients.stream()
                .map(c -> {
                    ClientDTO dto = new ClientDTO();
                    dto.setId(c.getId());
                    dto.setFullName(c.getFullName());
                    dto.setAge(c.getAge());
                    dto.setPhone(c.getPhone());
                    // Не включаем psychologist или включаем только ID
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Получить клиента по ID
    @GetMapping("/{clientId}")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<?> getClient(
            @AuthenticationPrincipal Psychologist psychologist,
            @PathVariable Long clientId) {
        try {
            Client client = clientManagementService.getClient(psychologist, clientId);
            return ResponseEntity.ok(client);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Поиск клиентов
    @GetMapping("/search")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<List<Client>> searchClients(
            @AuthenticationPrincipal Psychologist psychologist,
            @RequestParam(required = false) String query) {
        List<Client> clients = clientManagementService.searchClients(psychologist, query);
        return ResponseEntity.ok(clients);
    }

    // Получить статистику по клиенту
    @GetMapping("/{clientId}/stats")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<?> getClientStats(
            @AuthenticationPrincipal Psychologist psychologist,
            @PathVariable Long clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        try {
            ClientStats stats = clientManagementService.getClientStats(psychologist, clientId, from, to);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Получить активность клиента
    @GetMapping("/{clientId}/activity")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<?> getClientActivity(
            @AuthenticationPrincipal Psychologist psychologist,
            @PathVariable Long clientId,
            @RequestParam(defaultValue = "30") int days) {
        try {
            ClientActivity activity = clientManagementService.getClientActivity(psychologist, clientId, days);
            return ResponseEntity.ok(activity);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(e.getMessage()));
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

    @Data
    public static class ClientStats {
        private long totalSessions;
        private long completedSessions;
        private long cancelledSessions;
        private long journalEntries;
        private long activeRecommendations;
        private long completedRecommendations;
        private double attendanceRate; // процент посещенных сеансов
    }

    @Data
    public static class ClientActivity {
        private List<SessionActivity> recentSessions;
        private List<JournalActivity> recentJournalEntries;
        private List<RecommendationActivity> recentRecommendations;
        private MessageActivity messageActivity;
    }

    @Data
    public static class SessionActivity {
        private LocalDateTime date;
        private String status;
        private String description;
    }

    @Data
    public static class JournalActivity {
        private LocalDateTime date;
        private String mood;
        private int entryLength;
    }

    @Data
    public static class RecommendationActivity {
        private LocalDateTime date;
        private String title;
        private boolean completed;
    }

    @Data
    public static class MessageActivity {
        private long totalMessages;
        private long unreadMessages;
        private LocalDateTime lastMessageDate;
    }
}