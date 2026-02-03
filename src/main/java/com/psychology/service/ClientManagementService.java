package com.psychology.service;

import com.psychology.controller.ClientManagementController.*;
import com.psychology.model.entity.*;
import com.psychology.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientManagementService {

    private final ClientRepository clientRepository;
    private final SessionRepository sessionRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final RecommendationRepository recommendationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;

    public List<Client> getAllClients(Psychologist psychologist) {
        return clientRepository.findByPsychologistId(psychologist.getId());
    }

    public Client getClient(Psychologist psychologist, Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (!client.getPsychologist().getId().equals(psychologist.getId())) {
            throw new RuntimeException("Client does not belong to this psychologist");
        }

        return client;
    }

    public List<Client> searchClients(Psychologist psychologist, String query) {
        List<Client> allClients = clientRepository.findByPsychologistId(psychologist.getId());

        if (query == null || query.trim().isEmpty()) {
            return allClients;
        }

        String searchQuery = query.toLowerCase().trim();

        return allClients.stream()
                .filter(client ->
                        client.getFullName().toLowerCase().contains(searchQuery) ||
                                (client.getPhone() != null && client.getPhone().contains(searchQuery))
                )
                .collect(Collectors.toList());
    }

    public ClientStats getClientStats(Psychologist psychologist, Long clientId,
                                      LocalDateTime from, LocalDateTime to) {
        Client client = getClient(psychologist, clientId);

        ClientStats stats = new ClientStats();

        // Сеансы
        List<Session> sessions = (from != null && to != null) ?
                sessionRepository.findByClientIdAndScheduledAtBetween(clientId, from, to) :
                sessionRepository.findByClientIdOrderByScheduledAtDesc(clientId);

        stats.setTotalSessions(sessions.size());
        stats.setCompletedSessions((long) sessions.stream()
                .filter(s -> s.getStatus() == Session.SessionStatus.COMPLETED)
                .count());
        stats.setCancelledSessions((long) sessions.stream()
                .filter(s -> s.getStatus() == Session.SessionStatus.CANCELLED)
                .count());

        // Процент посещаемости
        long scheduledSessions = sessions.stream()
                .filter(s -> s.getStatus() == Session.SessionStatus.SCHEDULED ||
                        s.getStatus() == Session.SessionStatus.CONFIRMED)
                .count();
        long attendedSessions = sessions.stream()
                .filter(s -> s.getStatus() == Session.SessionStatus.COMPLETED)
                .count();

        if (scheduledSessions > 0) {
            stats.setAttendanceRate((double) attendedSessions / scheduledSessions * 100);
        } else {
            stats.setAttendanceRate(0);
        }

        // Записи в дневнике
        List<JournalEntry> journalEntries = (from != null && to != null) ?
                journalEntryRepository.findByClientId(clientId).stream()
                        .filter(e -> !e.getCreatedAt().isBefore(from) && !e.getCreatedAt().isAfter(to))
                        .collect(Collectors.toList()) :
                journalEntryRepository.findByClientIdOrderByCreatedAtDesc(clientId);

        stats.setJournalEntries(journalEntries.size());

        // Рекомендации
        List<Recommendation> recommendations = recommendationRepository.findByClientId(clientId);
        stats.setActiveRecommendations(recommendations.stream()
                .filter(r -> !r.isCompleted())
                .count());
        stats.setCompletedRecommendations(recommendations.stream()
                .filter(Recommendation::isCompleted)
                .count());

        return stats;
    }

    public ClientActivity getClientActivity(Psychologist psychologist, Long clientId, int days) {
        Client client = getClient(psychologist, clientId);

        ClientActivity activity = new ClientActivity();
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        // Недавние сеансы
        List<Session> recentSessions = sessionRepository.findByClientIdOrderByScheduledAtDesc(clientId)
                .stream()
                .filter(s -> s.getScheduledAt().isAfter(since))
                .limit(10)
                .collect(Collectors.toList());

        activity.setRecentSessions(recentSessions.stream()
                .map(session -> {
                    SessionActivity sa = new SessionActivity();
                    sa.setDate(session.getScheduledAt());
                    sa.setStatus(session.getStatus().name());
                    sa.setDescription(session.getDescription());
                    return sa;
                })
                .collect(Collectors.toList()));

        // Недавние записи в дневнике
        List<JournalEntry> recentJournalEntries = journalEntryRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .filter(e -> e.getCreatedAt().isAfter(since))
                .limit(10)
                .collect(Collectors.toList());

        activity.setRecentJournalEntries(recentJournalEntries.stream()
                .map(entry -> {
                    JournalActivity ja = new JournalActivity();
                    ja.setDate(entry.getCreatedAt());
                    ja.setMood(entry.getMood());
                    ja.setEntryLength(entry.getContent().length());
                    return ja;
                })
                .collect(Collectors.toList()));

        // Недавние рекомендации
        List<Recommendation> recentRecommendations = recommendationRepository.findByClientId(clientId)
                .stream()
                .filter(r -> r.getCreatedAt().isAfter(since))
                .sorted(Comparator.comparing(Recommendation::getCreatedAt).reversed())
                .limit(10)
                .collect(Collectors.toList());

        activity.setRecentRecommendations(recentRecommendations.stream()
                .map(rec -> {
                    RecommendationActivity ra = new RecommendationActivity();
                    ra.setDate(rec.getCreatedAt());
                    ra.setTitle(rec.getTitle());
                    ra.setCompleted(rec.isCompleted());
                    return ra;
                })
                .collect(Collectors.toList()));

        // Активность в чате
        MessageActivity messageActivity = new MessageActivity();
        List<Message> messages = messageRepository.findAllUserMessages(clientId)
                .stream()
                .filter(m -> m.getSentAt().isAfter(since))
                .collect(Collectors.toList());

        messageActivity.setTotalMessages(messages.size());
        messageActivity.setUnreadMessages(messages.stream()
                .filter(m -> !m.isRead() && m.getReceiver().getId().equals(clientId))
                .count());

        messages.stream()
                .max(Comparator.comparing(Message::getSentAt))
                .ifPresent(lastMessage -> messageActivity.setLastMessageDate(lastMessage.getSentAt()));

        activity.setMessageActivity(messageActivity);

        return activity;
    }

    @Transactional
    public void deleteClient(Psychologist psychologist, Long clientId) {
        Client client = getClient(psychologist, clientId);

        // Удаляем связанные данные
        messageRepository.deleteAllByUserId(clientId);
        notificationRepository.deleteByUserId(clientId);
        recommendationRepository.deleteByClientId(clientId);
        journalEntryRepository.deleteByClientId(clientId);
        sessionRepository.deleteByClientId(clientId);

        // Удаляем клиента (и запись в users)
        clientRepository.delete(client);
    }
}
