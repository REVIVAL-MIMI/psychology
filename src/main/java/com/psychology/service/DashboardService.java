package com.psychology.service;

import com.psychology.controller.DashboardController.*;
import com.psychology.model.entity.*;
import com.psychology.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final SessionRepository sessionRepository;
    private final ClientRepository clientRepository;
    private final MessageRepository messageRepository;
    private final RecommendationRepository recommendationRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final NotificationService notificationService;
    private static final List<Session.SessionStatus> UPCOMING_STATUSES = List.of(
            Session.SessionStatus.SCHEDULED,
            Session.SessionStatus.CONFIRMED,
            Session.SessionStatus.IN_PROGRESS,
            Session.SessionStatus.RESCHEDULED
    );

    public PsychologistDashboard getPsychologistDashboard(Psychologist psychologist) {
        PsychologistDashboard dashboard = new PsychologistDashboard();

        Long psychologistId = psychologist.getId();

        // Общее количество клиентов
        List<Client> allClients = clientRepository.findByPsychologistId(psychologistId);
        dashboard.setTotalClients(allClients.size());

        // Активные клиенты (с сеансами в ближайшие 30 дней) — один агрегирующий запрос
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysFromNow = now.plusDays(30);
        long activeClients = sessionRepository.countDistinctClientIdsByPsychologistIdAndScheduledAtBetweenAndStatusIn(
                psychologistId,
                now,
                thirtyDaysFromNow,
                UPCOMING_STATUSES
        );
        dashboard.setActiveClients(activeClients);

        // Сеансы на сегодня
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1);
        long sessionsToday = sessionRepository.countByPsychologistIdAndScheduledAtBetweenAndStatusIn(
                psychologistId,
                startOfToday,
                endOfToday,
                UPCOMING_STATUSES
        );
        dashboard.setUpcomingSessionsToday(sessionsToday);

        // Сеансы на эту неделю
        LocalDateTime startOfWeek = LocalDate.now().atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusDays(7);
        long sessionsThisWeek = sessionRepository.countByPsychologistIdAndScheduledAtBetweenAndStatusIn(
                psychologistId,
                startOfWeek,
                endOfWeek,
                UPCOMING_STATUSES
        );
        dashboard.setUpcomingSessionsThisWeek(sessionsThisWeek);

        // Невыполненные рекомендации — одним запросом
        long pendingRecommendations = recommendationRepository.countPendingByPsychologistId(psychologistId);
        dashboard.setPendingRecommendations(pendingRecommendations);

        // Непрочитанные сообщения у сотрудников этого психолога — одним запросом
        long unreadMessages = messageRepository.countUnreadForPsychologistClients(psychologistId);
        dashboard.setUnreadMessages(unreadMessages);

        // Ближайшие 5 сеансов с предзагрузкой связей client/psychologist
        List<Session> nextSessions = sessionRepository.findByPsychologistIdAndScheduledAtBetweenAndStatusInWithParticipants(
                        psychologistId,
                        now,
                        now.plusDays(90),
                        UPCOMING_STATUSES
                )
                .stream()
                .limit(5)
                .collect(Collectors.toList());
        dashboard.setNextSessions(nextSessions);

        // Статистика за текущий месяц
        dashboard.setMonthlyStats(getMonthlyStats(psychologistId));

        return dashboard;
    }

    public ClientDashboard getClientDashboard(Client client) {
        ClientDashboard dashboard = new ClientDashboard();

        // Информация о психологе
        Psychologist psychologist = client.getPsychologist();
        PsychologistInfo psychologistInfo = new PsychologistInfo();
        psychologistInfo.setId(psychologist.getId());
        psychologistInfo.setFullName(psychologist.getFullName());
        psychologistInfo.setSpecialization(psychologist.getSpecialization());
        psychologistInfo.setEmail(psychologist.getEmail());
        psychologistInfo.setOrganizationName(psychologist.getOrganizationName());
        psychologistInfo.setServiceFormat(psychologist.getServiceFormat());
        dashboard.setPsychologist(psychologistInfo);

        // Следующий сеанс
        LocalDateTime now = LocalDateTime.now();
        List<Session> upcomingSessions = sessionRepository.findByClientIdAndScheduledAtBetweenAndStatusInWithParticipants(
                client.getId(),
                now,
                now.plusDays(90),
                UPCOMING_STATUSES
        );

        if (!upcomingSessions.isEmpty()) {
            dashboard.setNextSession(upcomingSessions.get(0));
            dashboard.setUpcomingSessions(upcomingSessions.stream().limit(5).collect(Collectors.toList()));
        }

        // Непрочитанные сообщения
        long unreadMessages = messageRepository.countByReceiverIdAndReadFalse(client.getId());
        dashboard.setUnreadMessages(unreadMessages);

        // Активные рекомендации
        long pendingRecommendations = recommendationRepository.countByClientIdAndCompleted(client.getId(), false);
        dashboard.setPendingRecommendations(pendingRecommendations);

        // Записи в дневнике за текущий месяц
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long journalEntriesThisMonth = journalEntryRepository.countByClientIdAndCreatedAtBetween(
                client.getId(), startOfMonth, now);
        dashboard.setJournalEntriesThisMonth(journalEntriesThisMonth);

        // Последние уведомления
        List<NotificationService.NotificationDTO> recentNotifications =
                notificationService.getUserNotifications(client, 0, 5)
                        .stream()
                        .map(notificationService::convertToDTO)
                        .collect(Collectors.toList());
        dashboard.setRecentNotifications(recentNotifications);

        return dashboard;
    }

    public PsychologistStats getPsychologistStats(Psychologist psychologist, LocalDateTime start, LocalDateTime end) {
        PsychologistStats stats = new PsychologistStats();
        Long psychologistId = psychologist.getId();

        List<Client> clients = clientRepository.findByPsychologistId(psychologistId);

        // Все сеансы за период
        List<Session> allSessionsInPeriod = sessionRepository.findByPsychologistIdAndScheduledAtBetween(
                psychologistId, start, end);

        stats.setTotalSessions(allSessionsInPeriod.size());

        // Завершенные сеансы
        long completedSessions = allSessionsInPeriod.stream()
                .filter(session -> session.getStatus() == Session.SessionStatus.COMPLETED)
                .count();
        stats.setCompletedSessions(completedSessions);

        // Отмененные сеансы
        long cancelledSessions = allSessionsInPeriod.stream()
                .filter(session -> session.getStatus() == Session.SessionStatus.CANCELLED)
                .count();
        stats.setCancelledSessions(cancelledSessions);

        // Новые клиенты за период
        long newClients = clients.stream()
                .filter(client -> client.getLinkedAt() != null &&
                        !client.getLinkedAt().isBefore(start) &&
                        !client.getLinkedAt().isAfter(end))
                .count();
        stats.setNewClients(newClients);

        // Среднее количество сеансов на клиента
        if (!clients.isEmpty()) {
            double avgSessions = (double) allSessionsInPeriod.size() / clients.size();
            stats.setAverageSessionsPerClient(avgSessions);
        } else {
            stats.setAverageSessionsPerClient(0);
        }

        // Количество сеансов по месяцам
        Map<YearMonth, Long> sessionsByMonth = allSessionsInPeriod.stream()
                .collect(Collectors.groupingBy(
                        session -> YearMonth.from(session.getScheduledAt()),
                        Collectors.counting()
                ));

        List<MonthlySessionCount> monthlyCounts = sessionsByMonth.entrySet().stream()
                .map(entry -> {
                    MonthlySessionCount count = new MonthlySessionCount();
                    count.setMonth(entry.getKey().format(DateTimeFormatter.ofPattern("MMM yyyy")));
                    count.setCount(entry.getValue());
                    return count;
                })
                .sorted(Comparator.comparing(MonthlySessionCount::getMonth))
                .collect(Collectors.toList());

        stats.setMonthlySessionCounts(monthlyCounts);

        return stats;
    }

    public List<Session> getUpcomingSessions(Psychologist psychologist, int daysAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(daysAhead);

        return sessionRepository.findByPsychologistIdAndScheduledAtBetweenAndStatusInWithParticipants(
                psychologist.getId(),
                now,
                endDate,
                UPCOMING_STATUSES
        );
    }

    public List<Client> getActiveClients(Psychologist psychologist) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysFromNow = now.plusDays(30);

        return clientRepository.findActiveByPsychologistIdAndDateRange(
                psychologist.getId(),
                now,
                thirtyDaysFromNow,
                UPCOMING_STATUSES
        );
    }

    private MonthlyStats getMonthlyStats(Long psychologistId) {
        MonthlyStats stats = new MonthlyStats();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // Сеансы за текущий месяц
        List<Session> monthlySessions = sessionRepository.findByPsychologistIdAndScheduledAtBetween(
                psychologistId, startOfMonth, now);

        stats.setSessionsCompleted((long) monthlySessions.stream()
                .filter(s -> s.getStatus() == Session.SessionStatus.COMPLETED)
                .count());

        stats.setSessionsScheduled((long) monthlySessions.stream()
                .filter(s -> s.getStatus() == Session.SessionStatus.SCHEDULED ||
                        s.getStatus() == Session.SessionStatus.CONFIRMED)
                .count());

        // Новые клиенты за текущий месяц
        List<Client> monthlyClients = clientRepository.findByPsychologistId(psychologistId)
                .stream()
                .filter(client -> client.getLinkedAt() != null &&
                        !client.getLinkedAt().isBefore(startOfMonth) &&
                        !client.getLinkedAt().isAfter(now))
                .collect(Collectors.toList());

        stats.setNewClients((long) monthlyClients.size());

        // Revenue можно добавить позже, если будет платежная система

        return stats;
    }
}
