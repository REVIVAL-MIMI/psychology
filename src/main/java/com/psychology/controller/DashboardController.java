package com.psychology.controller;

import com.psychology.model.entity.Client;
import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.Session;
import com.psychology.service.DashboardService;
import com.psychology.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // Дашборд для психолога
    @GetMapping("/psychologist")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<PsychologistDashboard> getPsychologistDashboard(
            @AuthenticationPrincipal Psychologist psychologist) {
        PsychologistDashboard dashboard = dashboardService.getPsychologistDashboard(psychologist);
        return ResponseEntity.ok(dashboard);
    }

    // Дашборд для клиента
    @GetMapping("/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientDashboard> getClientDashboard(
            @AuthenticationPrincipal Client client) {
        ClientDashboard dashboard = dashboardService.getClientDashboard(client);
        return ResponseEntity.ok(dashboard);
    }

    // Статистика за период (для психолога)
    @GetMapping("/psychologist/stats")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<PsychologistStats> getPsychologistStats(
            @AuthenticationPrincipal Psychologist psychologist,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        PsychologistStats stats = dashboardService.getPsychologistStats(psychologist, start, end);
        return ResponseEntity.ok(stats);
    }

    // Ближайшие сеансы для психолога
    @GetMapping("/psychologist/upcoming-sessions")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<List<Session>> getUpcomingSessions(
            @AuthenticationPrincipal Psychologist psychologist,
            @RequestParam(defaultValue = "7") int daysAhead) {
        List<Session> sessions = dashboardService.getUpcomingSessions(psychologist, daysAhead);
        return ResponseEntity.ok(sessions);
    }

    // Активные клиенты (с сеансами в ближайшие 30 дней)
    @GetMapping("/psychologist/active-clients")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<List<Client>> getActiveClients(@AuthenticationPrincipal Psychologist psychologist) {
        List<Client> clients = dashboardService.getActiveClients(psychologist);
        return ResponseEntity.ok(clients);
    }

    @Data
    public static class PsychologistDashboard {
        private long totalClients;
        private long activeClients; // с сеансами в ближайшие 30 дней
        private long upcomingSessionsToday;
        private long upcomingSessionsThisWeek;
        private long pendingRecommendations; // невыполненные рекомендации
        private long unreadMessages;
        private List<Session> nextSessions; // ближайшие 5 сеансов
        private MonthlyStats monthlyStats;
    }

    @Data
    public static class ClientDashboard {
        private PsychologistInfo psychologist;
        private Session nextSession;
        private long unreadMessages;
        private long pendingRecommendations; // активные рекомендации
        private long journalEntriesThisMonth;
        private List<Session> upcomingSessions; // ближайшие 5 сеансов
        private List<NotificationService.NotificationDTO> recentNotifications;
    }

    @Data
    public static class PsychologistStats {
        private long totalSessions;
        private long completedSessions;
        private long cancelledSessions;
        private long newClients;
        private double averageSessionsPerClient;
        private List<MonthlySessionCount> monthlySessionCounts;
    }

    @Data
    public static class PsychologistInfo {
        private Long id;
        private String fullName;
        private String specialization;
        private String email;
    }

    @Data
    public static class MonthlyStats {
        private long sessionsCompleted;
        private long sessionsScheduled;
        private long newClients;
        private double revenue; // если будет система оплаты
    }

    @Data
    public static class MonthlySessionCount {
        private String month;
        private long count;
    }
}
