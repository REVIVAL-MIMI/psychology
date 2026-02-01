package com.psychology.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class SimpleWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public SimpleWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.test")
    public void testMessage(@Payload String message) {
        log.info("Received test message: {}", message);
        messagingTemplate.convertAndSend("/topic/test", "Echo: " + message);
    }
}