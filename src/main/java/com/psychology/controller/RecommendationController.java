package com.psychology.controller;

import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.Client;
import com.psychology.model.entity.Recommendation;
import com.psychology.service.RecommendationService;
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

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    // Психолог создает рекомендацию для клиента
    @PostMapping
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<?> createRecommendation(
            @AuthenticationPrincipal Psychologist psychologist,
            @RequestBody RecommendationRequest request) {
        try {
            Recommendation recommendation = recommendationService.createRecommendation(psychologist, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(recommendation);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Психолог обновляет рекомендацию
    @PutMapping("/{recommendationId}")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<?> updateRecommendation(
            @AuthenticationPrincipal Psychologist psychologist,
            @PathVariable Long recommendationId,
            @RequestBody RecommendationUpdateRequest request) {
        try {
            Recommendation recommendation = recommendationService.updateRecommendation(psychologist, recommendationId, request);
            return ResponseEntity.ok(recommendation);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Психолог помечает рекомендацию как выполненную
    @PostMapping("/{recommendationId}/complete")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<?> completeRecommendation(
            @AuthenticationPrincipal Psychologist psychologist,
            @PathVariable Long recommendationId) {
        try {
            Recommendation recommendation = recommendationService.completeRecommendation(psychologist, recommendationId);
            return ResponseEntity.ok(recommendation);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Клиент помечает рекомендацию как выполненную
    @PostMapping("/{recommendationId}/client-complete")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> markAsCompletedByClient(
            @AuthenticationPrincipal Client client,
            @PathVariable Long recommendationId) {
        try {
            Recommendation recommendation = recommendationService.markAsCompletedByClient(client, recommendationId);
            return ResponseEntity.ok(recommendation);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Психолог удаляет рекомендацию
    @DeleteMapping("/{recommendationId}")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<?> deleteRecommendation(
            @AuthenticationPrincipal Psychologist psychologist,
            @PathVariable Long recommendationId) {
        try {
            recommendationService.deleteRecommendation(psychologist, recommendationId);
            return ResponseEntity.ok(new ApiResponse("Recommendation deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Психолог получает рекомендации для клиента
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<?> getClientRecommendations(
            @AuthenticationPrincipal Psychologist psychologist,
            @PathVariable Long clientId,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        try {
            List<Recommendation> recommendations = recommendationService.getClientRecommendations(
                    psychologist, clientId, completed, overdue, from, to);
            return ResponseEntity.ok(recommendations);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Клиент получает свои рекомендации
    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<Recommendation>> getMyRecommendations(
            @AuthenticationPrincipal Client client,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<Recommendation> recommendations = recommendationService.getMyRecommendations(
                client, completed, overdue, from, to);
        return ResponseEntity.ok(recommendations);
    }

    // Психолог получает все просроченные рекомендации
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<List<Recommendation>> getOverdueRecommendations(
            @AuthenticationPrincipal Psychologist psychologist) {
        List<Recommendation> recommendations = recommendationService.getOverdueRecommendations(psychologist);
        return ResponseEntity.ok(recommendations);
    }

    // Статистика по рекомендациям
    @GetMapping("/stats")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<RecommendationStats> getRecommendationStats(
            @AuthenticationPrincipal Psychologist psychologist) {
        RecommendationStats stats = recommendationService.getRecommendationStats(psychologist);
        return ResponseEntity.ok(stats);
    }

    @Data
    public static class RecommendationRequest {
        private Long clientId;
        private String title;
        private String content;
        private LocalDateTime deadline;
        private Integer priority; // 1-5, где 5 - наивысший
        private List<String> categories;
    }

    @Data
    public static class RecommendationUpdateRequest {
        private String title;
        private String content;
        private LocalDateTime deadline;
        private Integer priority;
        private Boolean completed;
        private List<String> categories;
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
    public static class RecommendationStats {
        private long totalRecommendations;
        private long completedRecommendations;
        private long pendingRecommendations;
        private long overdueRecommendations;
        private double completionRate;
    }
}