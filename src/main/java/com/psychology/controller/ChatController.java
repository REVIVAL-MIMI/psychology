package com.psychology.controller;

import com.psychology.dto.ChatDTO;
import com.psychology.model.entity.User;
import com.psychology.service.ChatService;
import com.psychology.service.NotificationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @AuthenticationPrincipal User sender,
            @RequestBody ChatDTO.SendMessageRequest request) {
        try {
            ChatDTO.MessageResponse response = chatService.sendMessage(sender, request);

            // Отправляем уведомление через NotificationService
            notificationService.sendNewMessageNotification(response);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @GetMapping("/conversation/{userId}")
    public ResponseEntity<?> getConversation(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long userId) {
        try {
            List<ChatDTO.MessageResponse> conversation =
                    chatService.getConversation(currentUser, userId);
            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentMessages(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String since) {
        try {
            LocalDateTime sinceTime = since != null ?
                    LocalDateTime.parse(since) : LocalDateTime.now().minusDays(7);

            List<ChatDTO.MessageResponse> messages =
                    chatService.getRecentMessages(user, sinceTime);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @PostMapping("/read/{messageId}")
    public ResponseEntity<?> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long messageId) {
        try {
            chatService.markAsRead(user, messageId);
            return ResponseEntity.ok(new ApiResponse("Message marked as read"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal User user) {
        try {
            Long count = chatService.getUnreadCount(user);
            // Создаем ответ вручную
            var response = new ChatDTO.UnreadCountResponse();
            response.setUnreadCount(count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadMessages(@AuthenticationPrincipal User user) {
        try {
            List<ChatDTO.MessageResponse> messages = chatService.getUnreadMessages(user);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // УДАЛЯЕМ WebSocket методы, они теперь в WebSocketChatController

    @Data
    @AllArgsConstructor
    public static class ApiResponse {
        private String message;
        private LocalDateTime timestamp;

        public ApiResponse(String message) {
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }
    }
}