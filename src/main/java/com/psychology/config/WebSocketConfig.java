package com.psychology.config;

import com.psychology.security.JwtTokenProvider;
import com.psychology.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:8080")
                .withSockJS();

        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:8080");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authorization = accessor.getNativeHeader("Authorization");

                    if (authorization != null && !authorization.isEmpty()) {
                        String token = authorization.get(0);

                        if (token != null && token.startsWith("Bearer ")) {
                            token = token.substring(7);

                            try {
                                if (jwtTokenProvider.validateToken(token)) {
                                    String phone = jwtTokenProvider.extractUsername(token);

                                    var user = userRepository.findByPhone(phone)
                                            .orElseThrow(() -> new RuntimeException("User not found"));

                                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                                            user, null, user.getAuthorities()
                                    );
                                    accessor.setUser(authentication);

                                    log.info("WebSocket authenticated user: {}", phone);
                                } else {
                                    log.warn("Invalid WebSocket token");
                                    return null;
                                }
                            } catch (Exception e) {
                                log.error("Error validating WebSocket token: {}", e.getMessage());
                                return null;
                            }
                        } else {
                            log.warn("No Bearer token in WebSocket headers");
                            return null;
                        }
                    } else {
                        log.warn("No Authorization header in WebSocket connection");
                        return null;
                    }
                }
                return message;
            }
        });
    }

    @Override
    public void configureWebSocketTransport(org.springframework.web.socket.config.annotation.WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(5 * 1024 * 1024); // 5MB
        registration.setSendTimeLimit(20 * 1000); // 20 seconds
        registration.setSendBufferSizeLimit(5 * 1024 * 1024); // 5MB
    }
}