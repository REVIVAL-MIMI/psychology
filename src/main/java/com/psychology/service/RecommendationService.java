package com.psychology.service;

import com.psychology.controller.RecommendationController.RecommendationRequest;
import com.psychology.controller.RecommendationController.RecommendationUpdateRequest;
import com.psychology.controller.RecommendationController.RecommendationStats;
import com.psychology.model.entity.Recommendation;
import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.Client;
import com.psychology.repository.RecommendationRepository;
import com.psychology.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;

    @Transactional
    public Recommendation createRecommendation(Psychologist psychologist, RecommendationRequest request) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // Проверяем, что клиент принадлежит психологу
        if (!client.getPsychologist().getId().equals(psychologist.getId())) {
            throw new RuntimeException("Client does not belong to this psychologist");
        }

        Recommendation recommendation = new Recommendation();
        recommendation.setPsychologist(psychologist);
        recommendation.setClient(client);
        recommendation.setTitle(request.getTitle());
        recommendation.setContent(request.getContent());
        recommendation.setDeadline(request.getDeadline());
        recommendation.setPriority(request.getPriority() != null ? request.getPriority() : 3);
        recommendation.setCategories(request.getCategories());
        recommendation.setCompleted(false);
        recommendation.setCreatedAt(LocalDateTime.now());

        Recommendation saved = recommendationRepository.save(recommendation);

        // Создаем уведомление для клиента
        notificationService.createNotification(
                client,
                com.psychology.model.entity.Notification.NotificationType.NEW_RECOMMENDATION,
                "Новая рекомендация",
                String.format("У вас новая рекомендация от психолога: %s", request.getTitle())
        );

        return saved;
    }

    @Transactional
    public Recommendation updateRecommendation(Psychologist psychologist, Long recommendationId, RecommendationUpdateRequest request) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Recommendation not found"));

        // Проверяем, что рекомендация принадлежит психологу
        if (!recommendation.getPsychologist().getId().equals(psychologist.getId())) {
            throw new RuntimeException("Recommendation does not belong to this psychologist");
        }

        if (request.getTitle() != null) {
            recommendation.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            recommendation.setContent(request.getContent());
        }
        if (request.getDeadline() != null) {
            recommendation.setDeadline(request.getDeadline());
        }
        if (request.getPriority() != null) {
            recommendation.setPriority(request.getPriority());
        }
        if (request.getCompleted() != null) {
            recommendation.setCompleted(request.getCompleted());
            if (request.getCompleted()) {
                recommendation.setCompletedAt(LocalDateTime.now());
            }
        }
        if (request.getCategories() != null) {
            recommendation.setCategories(request.getCategories());
        }

        return recommendationRepository.save(recommendation);
    }

    @Transactional
    public Recommendation completeRecommendation(Psychologist psychologist, Long recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Recommendation not found"));

        if (!recommendation.getPsychologist().getId().equals(psychologist.getId())) {
            throw new RuntimeException("Recommendation does not belong to this psychologist");
        }

        recommendation.setCompleted(true);
        recommendation.setCompletedAt(LocalDateTime.now());

        return recommendationRepository.save(recommendation);
    }

    @Transactional
    public Recommendation markAsCompletedByClient(Client client, Long recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Recommendation not found"));

        if (!recommendation.getClient().getId().equals(client.getId())) {
            throw new RuntimeException("Recommendation does not belong to this client");
        }

        recommendation.setCompleted(true);
        recommendation.setCompletedAt(LocalDateTime.now());
        recommendation.setCompletedByClient(true);

        return recommendationRepository.save(recommendation);
    }

    @Transactional
    public void deleteRecommendation(Psychologist psychologist, Long recommendationId) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Recommendation not found"));

        if (!recommendation.getPsychologist().getId().equals(psychologist.getId())) {
            throw new RuntimeException("Recommendation does not belong to this psychologist");
        }

        recommendationRepository.delete(recommendation);
    }

    public List<Recommendation> getClientRecommendations(Psychologist psychologist, Long clientId,
                                                         Boolean completed, Boolean overdue,
                                                         LocalDateTime from, LocalDateTime to) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (!client.getPsychologist().getId().equals(psychologist.getId())) {
            throw new RuntimeException("Client does not belong to this psychologist");
        }

        List<Recommendation> recommendations = recommendationRepository.findByClientId(clientId);

        return filterRecommendations(recommendations, completed, overdue, from, to);
    }

    public List<Recommendation> getMyRecommendations(Client client, Boolean completed,
                                                     Boolean overdue, LocalDateTime from,
                                                     LocalDateTime to) {
        List<Recommendation> recommendations = recommendationRepository.findByClientId(client.getId());
        return filterRecommendations(recommendations, completed, overdue, from, to);
    }

    public List<Recommendation> getOverdueRecommendations(Psychologist psychologist) {
        List<Client> clients = clientRepository.findByPsychologistId(psychologist.getId());
        List<Recommendation> allRecommendations = new ArrayList<>();

        for (Client client : clients) {
            List<Recommendation> clientRecs = recommendationRepository.findByClientId(client.getId());
            allRecommendations.addAll(clientRecs);
        }

        LocalDateTime now = LocalDateTime.now();
        return allRecommendations.stream()
                .filter(r -> !r.isCompleted() && r.getDeadline() != null && r.getDeadline().isBefore(now))
                .sorted((r1, r2) -> r1.getDeadline().compareTo(r2.getDeadline()))
                .collect(Collectors.toList());
    }

    public RecommendationStats getRecommendationStats(Psychologist psychologist) {
        List<Client> clients = clientRepository.findByPsychologistId(psychologist.getId());
        List<Recommendation> allRecommendations = new ArrayList<>();

        for (Client client : clients) {
            allRecommendations.addAll(recommendationRepository.findByClientId(client.getId()));
        }

        RecommendationStats stats = new RecommendationStats();
        stats.setTotalRecommendations(allRecommendations.size());

        long completed = allRecommendations.stream().filter(Recommendation::isCompleted).count();
        stats.setCompletedRecommendations(completed);

        long pending = allRecommendations.size() - completed;
        stats.setPendingRecommendations(pending);

        LocalDateTime now = LocalDateTime.now();
        long overdue = allRecommendations.stream()
                .filter(r -> !r.isCompleted() && r.getDeadline() != null && r.getDeadline().isBefore(now))
                .count();
        stats.setOverdueRecommendations(overdue);

        stats.setCompletionRate(allRecommendations.isEmpty() ? 0 : (double) completed / allRecommendations.size() * 100);

        return stats;
    }

    private List<Recommendation> filterRecommendations(List<Recommendation> recommendations,
                                                       Boolean completed, Boolean overdue,
                                                       LocalDateTime from, LocalDateTime to) {
        return recommendations.stream()
                .filter(r -> completed == null || r.isCompleted() == completed)
                .filter(r -> {
                    if (overdue == null) return true;
                    LocalDateTime now = LocalDateTime.now();
                    if (overdue) {
                        return !r.isCompleted() && r.getDeadline() != null && r.getDeadline().isBefore(now);
                    } else {
                        return r.isCompleted() || r.getDeadline() == null || !r.getDeadline().isBefore(now);
                    }
                })
                .filter(r -> from == null || (r.getCreatedAt() != null && !r.getCreatedAt().isBefore(from)))
                .filter(r -> to == null || (r.getCreatedAt() != null && !r.getCreatedAt().isAfter(to)))
                .sorted((r1, r2) -> {
                    // Сначала просроченные, затем по приоритету, затем по дедлайну
                    LocalDateTime now = LocalDateTime.now();
                    boolean r1Overdue = !r1.isCompleted() && r1.getDeadline() != null && r1.getDeadline().isBefore(now);
                    boolean r2Overdue = !r2.isCompleted() && r2.getDeadline() != null && r2.getDeadline().isBefore(now);

                    if (r1Overdue && !r2Overdue) return -1;
                    if (!r1Overdue && r2Overdue) return 1;

                    int priorityCompare = Integer.compare(r2.getPriority(), r1.getPriority());
                    if (priorityCompare != 0) return priorityCompare;

                    if (r1.getDeadline() == null && r2.getDeadline() == null) return 0;
                    if (r1.getDeadline() == null) return 1;
                    if (r2.getDeadline() == null) return -1;

                    return r1.getDeadline().compareTo(r2.getDeadline());
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void checkAndNotifyOverdue() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);

        List<Recommendation> overdueRecommendations = recommendationRepository.findOverdueRecommendations(yesterday);

        for (Recommendation recommendation : overdueRecommendations) {
            // Отправляем уведомление клиенту
            notificationService.createNotification(
                    recommendation.getClient(),
                    com.psychology.model.entity.Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Просроченная рекомендация",
                    String.format("Рекомендация '%s' просрочена", recommendation.getTitle())
            );
        }
    }
}