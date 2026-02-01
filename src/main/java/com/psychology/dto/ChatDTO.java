package com.psychology.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ChatDTO {

    @Data
    public static class SendMessageRequest {
        private Long receiverId;
        private String content;
        private String attachmentUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageResponse {
        private Long id;
        private Long senderId;
        private String senderName;
        private String senderRole;
        private Long receiverId;
        private String receiverName;
        private String content;
        private String attachmentUrl;
        private boolean read;
        private LocalDateTime sentAt;
    }

    @Data
    public static class ConversationRequest {
        private Long otherUserId;
    }

    @Data
    public static class MarkAsReadRequest {
        private Long messageId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UnreadCountResponse {
        private Long unreadCount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatNotification {
        private Long messageId;
        private Long senderId;
        private String senderName;
        private String content;
        private LocalDateTime sentAt;
    }
}