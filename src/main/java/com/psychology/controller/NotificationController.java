package com.psychology.controller;

import com.psychology.model.entity.User;
import com.psychology.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<NotificationService.NotificationDTO> notifications =
                notificationService.getUserNotifications(user, page, size)
                        .stream()
                        .map(notificationService::convertToDTO)
                        .toList();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(@AuthenticationPrincipal User user) {
        List<NotificationService.NotificationDTO> notifications =
                notificationService.getUnreadNotifications(user)
                        .stream()
                        .map(notificationService::convertToDTO)
                        .toList();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal User user) {
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long notificationId) {
        try {
            notificationService.markAsRead(notificationId, user);
            return ResponseEntity.ok(new ApiResponse("Notification marked as read"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage()));
        }
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(new ApiResponse("All notifications marked as read"));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(
            @AuthenticationPrincipal User user,
            @PathVariable Long notificationId) {
        try {
            notificationService.deleteNotification(notificationId, user);
            return ResponseEntity.ok(new ApiResponse("Notification deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage()));
        }
    }

    @Data
    public static class UnreadCountResponse {
        private long unreadCount;

        public UnreadCountResponse(long unreadCount) {
            this.unreadCount = unreadCount;
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
}