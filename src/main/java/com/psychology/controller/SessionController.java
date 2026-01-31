package com.psychology.controller;

import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.Client;
import com.psychology.model.entity.Session;
import com.psychology.service.SessionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    // Психолог создает сеанс
    @PostMapping
    public ResponseEntity<?> createSession(
            @AuthenticationPrincipal Psychologist psychologist,
            @RequestBody SessionService.SessionRequest request) {
        try {
            Session session = sessionService.createSession(psychologist, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(session);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Психолог обновляет сеанс
    @PutMapping("/{sessionId}")
    public ResponseEntity<?> updateSession(
            @AuthenticationPrincipal Psychologist psychologist,
            @PathVariable Long sessionId,
            @RequestBody SessionService.SessionUpdateRequest request) {
        try {
            Session session = sessionService.updateSession(sessionId, psychologist, request);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Получить сеансы психолога
    @GetMapping("/psychologist")
    public ResponseEntity<List<Session>> getPsychologistSessions(
            @AuthenticationPrincipal Psychologist psychologist,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<Session> sessions = sessionService.getPsychologistSessions(psychologist, from, to);
        return ResponseEntity.ok(sessions);
    }

    // Получить сеансы клиента
    @GetMapping("/client")
    public ResponseEntity<List<Session>> getClientSessions(
            @AuthenticationPrincipal Client client,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<Session> sessions = sessionService.getClientSessions(client, from, to);
        return ResponseEntity.ok(sessions);
    }

    // Отменить сеанс
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<?> cancelSession(
            @AuthenticationPrincipal Object user,
            @PathVariable Long sessionId) {
        try {
            String userType = getUserType(user);
            Session session = sessionService.cancelSession(sessionId, userType, user);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    private String getUserType(Object user) {
        if (user instanceof Psychologist) {
            return "PSYCHOLOGIST";
        } else if (user instanceof Client) {
            return "CLIENT";
        }
        throw new RuntimeException("Unknown user type");
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