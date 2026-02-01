package com.psychology.service;

import com.psychology.dto.ChatDTO;
import com.psychology.model.entity.Message;
import com.psychology.model.entity.User;
import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.Client;
import com.psychology.repository.MessageRepository;
import com.psychology.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService; // Добавляем зависимость

    @Transactional
    public ChatDTO.MessageResponse sendMessage(User sender, ChatDTO.SendMessageRequest request) {
        log.info("Sending message from {} to {}", sender.getId(), request.getReceiverId());

        // Проверяем, что получатель существует
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        // Проверяем, что можно отправлять сообщения
        if (!canSendMessage(sender, receiver)) {
            throw new RuntimeException("Cannot send message to this user");
        }

        // Проверяем длину сообщения (макс 2000 символов по ТЗ)
        if (request.getContent().length() > 2000) {
            throw new RuntimeException("Message too long. Maximum 2000 characters");
        }

        // Сохраняем сообщение
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(request.getContent());
        message.setAttachmentUrl(request.getAttachmentUrl());
        message.setSentAt(LocalDateTime.now());

        messageRepository.save(message);

        // Создаем DTO для ответа
        ChatDTO.MessageResponse response = convertToDTO(message);

        // Отправляем уведомление через NotificationService
        notificationService.sendNewMessageNotification(response);

        log.info("Message sent successfully. ID: {}", message.getId());
        return response;
    }

    @Transactional(readOnly = true)
    public List<ChatDTO.MessageResponse> getConversation(User currentUser, Long otherUserId) {
        log.info("Getting conversation between {} and {}", currentUser.getId(), otherUserId);

        // Проверяем, что другой пользователь существует
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Проверяем, что можно видеть переписку
        if (!canSeeConversation(currentUser, otherUser)) {
            throw new RuntimeException("Cannot access this conversation");
        }

        List<Message> messages = messageRepository.findConversation(currentUser.getId(), otherUserId);

        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatDTO.MessageResponse> getRecentMessages(User user, LocalDateTime since) {
        log.info("Getting recent messages for user {} since {}", user.getId(), since);

        List<Message> messages = messageRepository.findRecentMessages(user.getId(), since);

        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(User user, Long messageId) {
        log.info("Marking message {} as read by user {}", messageId, user.getId());

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Проверяем, что пользователь - получатель сообщения
        if (!message.getReceiver().getId().equals(user.getId())) {
            throw new RuntimeException("Cannot mark this message as read");
        }

        message.setRead(true);
        messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(User user) {
        return messageRepository.countByReceiverIdAndReadFalse(user.getId());
    }

    @Transactional(readOnly = true)
    public List<ChatDTO.MessageResponse> getUnreadMessages(User user) {
        List<Message> messages = messageRepository.findUnreadMessages(user.getId());

        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private boolean canSendMessage(User sender, User receiver) {
        // Психолог может писать только своим клиентам и наоборот
        if (sender instanceof Psychologist && receiver instanceof Client) {
            Client client = (Client) receiver;
            return client.getPsychologist().getId().equals(sender.getId());
        } else if (sender instanceof Client && receiver instanceof Psychologist) {
            Client client = (Client) sender;
            return client.getPsychologist().getId().equals(receiver.getId());
        }
        return false;
    }

    private boolean canSeeConversation(User user1, User user2) {
        // Тот же принцип: видеть переписку могут только связанные психолог-клиент
        if (user1 instanceof Psychologist && user2 instanceof Client) {
            Client client = (Client) user2;
            return client.getPsychologist().getId().equals(user1.getId());
        } else if (user1 instanceof Client && user2 instanceof Psychologist) {
            Client client = (Client) user1;
            return client.getPsychologist().getId().equals(user2.getId());
        }
        return false;
    }

    private ChatDTO.MessageResponse convertToDTO(Message message) {
        ChatDTO.MessageResponse dto = new ChatDTO.MessageResponse();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(getUserName(message.getSender()));
        dto.setSenderRole(message.getSender().getRole().name());
        dto.setReceiverId(message.getReceiver().getId());
        dto.setReceiverName(getUserName(message.getReceiver()));
        dto.setContent(message.getContent());
        dto.setAttachmentUrl(message.getAttachmentUrl());
        dto.setRead(message.isRead());
        dto.setSentAt(message.getSentAt());
        return dto;
    }

    private String getUserName(User user) {
        if (user instanceof Psychologist) {
            return ((Psychologist) user).getFullName();
        } else if (user instanceof Client) {
            return ((Client) user).getFullName();
        }
        return "Unknown";
    }
}