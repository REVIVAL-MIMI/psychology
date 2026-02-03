package com.psychology.websocket;

import com.psychology.dto.CallDTO;
import com.psychology.model.entity.User;
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
public class WebSocketCallController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/call.offer")
    public void offer(@Payload CallDTO.Signal request, Authentication authentication) {
        sendSignal("offer", request, authentication);
    }

    @MessageMapping("/call.answer")
    public void answer(@Payload CallDTO.Signal request, Authentication authentication) {
        sendSignal("answer", request, authentication);
    }

    @MessageMapping("/call.ice")
    public void ice(@Payload CallDTO.Signal request, Authentication authentication) {
        sendSignal("ice", request, authentication);
    }

    @MessageMapping("/call.hangup")
    public void hangup(@Payload CallDTO.Signal request, Authentication authentication) {
        sendSignal("hangup", request, authentication);
    }

    private void sendSignal(String type, CallDTO.Signal request, Authentication authentication) {
        try {
            User sender = (User) authentication.getPrincipal();
            CallDTO.Signal signal = new CallDTO.Signal();
            signal.setType(type);
            signal.setSenderId(sender.getId());
            signal.setReceiverId(request.getReceiverId());
            signal.setSdp(request.getSdp());
            signal.setCandidate(request.getCandidate());
            signal.setSdpMid(request.getSdpMid());
            signal.setSdpMLineIndex(request.getSdpMLineIndex());
            signal.setReason(request.getReason());

            String destination = "/user/" + request.getReceiverId() + "/queue/call";
            messagingTemplate.convertAndSend(destination, signal);
        } catch (Exception e) {
            log.error("Error sending call signal: {}", e.getMessage());
        }
    }
}
