package com.psychology.scheduler;

import com.psychology.model.entity.Session;
import com.psychology.repository.SessionRepository;
import com.psychology.service.JournalService;
import com.psychology.service.NotificationService;
import com.psychology.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;
    private final JournalService journalService;
    private final RecommendationService recommendationService;

    // Проверка сеансов для напоминаний (каждые 30 минут)
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30 минут
    public void checkSessionReminders() {
        LocalDateTime now = LocalDateTime.now();

        // Напоминания за 24 часа
        LocalDateTime in24Hours = now.plusHours(24);
        List<Session> sessions24h = sessionRepository.findByScheduledAtBetween(
                in24Hours.minusMinutes(30), in24Hours.plusMinutes(30));

        sessions24h.forEach(session -> {
            // Создаем уведомление для клиента
            notificationService.createNotification(
                    session.getClient(),
                    com.psychology.model.entity.Notification.NotificationType.SESSION_REMINDER_24H,
                    "Напоминание о сеансе",
                    String.format("У вас запланирован сеанс через 24 часа: %s",
                            session.getScheduledAt().toLocalDate()),
                    session.getId(),
                    "SESSION"
            );

            // Создаем уведомление для психолога
            notificationService.createNotification(
                    session.getPsychologist(),
                    com.psychology.model.entity.Notification.NotificationType.SESSION_REMINDER_24H,
                    "Напоминание о сеансе",
                    String.format("У вас запланирован сеанс с %s через 24 часа",
                            session.getClient().getFullName()),
                    session.getId(),
                    "SESSION"
            );
        });

        // Напоминания за 1 час
        LocalDateTime in1Hour = now.plusHours(1);
        List<Session> sessions1h = sessionRepository.findByScheduledAtBetween(
                in1Hour.minusMinutes(15), in1Hour.plusMinutes(15));

        sessions1h.forEach(session -> {
            notificationService.createNotification(
                    session.getClient(),
                    com.psychology.model.entity.Notification.NotificationType.SESSION_REMINDER_1H,
                    "Скоро начнется сеанс",
                    "До начала сеанса остался 1 час",
                    session.getId(),
                    "SESSION"
            );

            notificationService.createNotification(
                    session.getPsychologist(),
                    com.psychology.model.entity.Notification.NotificationType.SESSION_REMINDER_1H,
                    "Скоро начнется сеанс",
                    String.format("До начала сеанса с %s остался 1 час",
                            session.getClient().getFullName()),
                    session.getId(),
                    "SESSION"
            );
        });
    }

    // Очистка старых записей дневника (раз в день)
    @Scheduled(cron = "0 0 2 * * ?") // В 2:00 ночи каждый день
    public void cleanupOldData() {
        log.info("Starting scheduled cleanup of old data");

        // Очистка записей дневника старше 3 лет
        journalService.cleanupOldEntries();

        // Очистка старых уведомлений
        notificationService.cleanupOldNotifications();

        log.info("Scheduled cleanup completed");
    }

    // Проверка просроченных рекомендаций (раз в день)
    @Scheduled(cron = "0 0 8 * * ?") // В 8:00 утра каждый день
    public void checkOverdueRecommendations() {
        log.info("Checking for overdue recommendations");
        recommendationService.checkAndNotifyOverdue();
    }
}