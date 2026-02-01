package com.psychology.websocket;

import com.psychology.dto.ChatDTO;
import com.psychology.dto.TypingNotificationDTO;
import com.psychology.model.entity.User;
import com.psychology.service.ChatService;
import com.psychology.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final ChatService chatService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatDTO.SendMessageRequest request,
                            Authentication authentication) {
        try {
            log.info("WebSocket send message from {}", authentication.getName());

            User sender = (User) authentication.getPrincipal();
            ChatDTO.MessageResponse response = chatService.sendMessage(sender, request);

            // Отправляем сообщение получателю
            String receiverDestination = "/user/" + request.getReceiverId() + "/queue/messages";
            messagingTemplate.convertAndSend(receiverDestination, response);

            // Отправляем подтверждение отправителю
            String senderDestination = "/user/" + sender.getId() + "/queue/messages";
            messagingTemplate.convertAndSend(senderDestination, response);

            // Отправляем уведомление через NotificationService
            notificationService.sendNewMessageNotification(response);

        } catch (Exception e) {
            log.error("Error sending message via WebSocket: {}", e.getMessage());
        }
    }

    @MessageMapping("/chat.typing")
    public void typingNotification(@Payload TypingRequest request,
                                   Authentication authentication) {
        try {
            User sender = (User) authentication.getPrincipal();
            notificationService.sendTypingNotification(
                    sender.getId(),
                    request.getReceiverId(),
                    request.isTyping()
            );

        } catch (Exception e) {
            log.error("Error sending typing notification: {}", e.getMessage());
        }
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload ChatDTO.MarkAsReadRequest request,
                           Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            chatService.markAsRead(user, request.getMessageId());

            // Уведомляем отправителя о прочтении через NotificationService
            notificationService.sendMessageReadNotification(request.getMessageId(), user.getId());

        } catch (Exception e) {
            log.error("Error marking message as read: {}", e.getMessage());
        }
    }

    @Data
    public static class TypingRequest {
        private Long receiverId;
        private boolean typing;
    }
}