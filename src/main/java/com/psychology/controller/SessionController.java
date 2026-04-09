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
            @AuthenticationPrincipal Object principal,
            @RequestBody SessionService.SessionRequest request) {
        try {
            Psychologist psychologist = extractPsychologist(principal);
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
            @AuthenticationPrincipal Object principal,
            @PathVariable Long sessionId,
            @RequestBody SessionService.SessionUpdateRequest request) {
        try {
            Psychologist psychologist = extractPsychologist(principal);
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
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Psychologist psychologist = extractPsychologist(principal);
        List<Session> sessions = sessionService.getPsychologistSessions(psychologist, from, to);
        return ResponseEntity.ok(sessions);
    }

    // Получить сеансы клиента
    @GetMapping("/client")
    public ResponseEntity<List<Session>> getClientSessions(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        Client client = extractClient(principal);
        List<Session> sessions = sessionService.getClientSessions(client, from, to);
        return ResponseEntity.ok(sessions);
    }

    // Свободные слоты для самозаписи сотрудника
    @GetMapping("/available-slots")
    public ResponseEntity<?> getAvailableSlotsForClient(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) Integer daysAhead) {
        try {
            Client client = extractClient(principal);
            List<SessionService.AvailableDaySlots> slots = sessionService.getAvailableSlotsForClient(client, daysAhead);
            return ResponseEntity.ok(slots);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Самозапись сотрудника на слот
    @PostMapping("/book")
    public ResponseEntity<?> bookByClient(
            @AuthenticationPrincipal Object principal,
            @RequestBody SessionService.ClientBookingRequest request) {
        try {
            Client client = extractClient(principal);
            Session session = sessionService.bookSessionByClient(client, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(session);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Мой график психолога на 2 недели (или иной диапазон)
    @GetMapping("/my-schedule")
    public ResponseEntity<?> getMySchedule(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) Integer daysAhead) {
        try {
            Psychologist psychologist = extractPsychologist(principal);
            List<SessionService.PsychologistScheduleDay> schedule = sessionService.getPsychologistSchedule(psychologist, daysAhead);
            return ResponseEntity.ok(schedule);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Обновление рабочих/нерабочих дней и часов психолога
    @PutMapping("/availability")
    public ResponseEntity<?> updateAvailability(
            @AuthenticationPrincipal Object principal,
            @RequestBody SessionService.UpdateAvailabilityRequest request,
            @RequestParam(required = false) Integer daysAhead) {
        try {
            Psychologist psychologist = extractPsychologist(principal);
            List<SessionService.PsychologistScheduleDay> schedule =
                    sessionService.updatePsychologistAvailability(psychologist, request, daysAhead);
            return ResponseEntity.ok(schedule);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
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

    private Psychologist extractPsychologist(Object principal) {
        if (principal instanceof Psychologist psychologist) {
            return psychologist;
        }
        throw new RuntimeException("Psychologist access required");
    }

    private Client extractClient(Object principal) {
        if (principal instanceof Client client) {
            return client;
        }
        throw new RuntimeException("Client access required");
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
