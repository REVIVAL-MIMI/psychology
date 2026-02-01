package com.psychology.dto;

import lombok.Data;

@Data
public class TypingNotificationDTO {
    private Long senderId;
    private boolean typing;

    public TypingNotificationDTO() {}

    public TypingNotificationDTO(Long senderId, boolean typing) {
        this.senderId = senderId;
        this.typing = typing;
    }
}