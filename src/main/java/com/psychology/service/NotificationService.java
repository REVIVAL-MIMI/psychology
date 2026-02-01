package com.psychology.service;

import com.psychology.dto.ChatDTO;
import com.psychology.dto.TypingNotificationDTO;
import com.psychology.model.entity.Notification;
import com.psychology.model.entity.User;
import com.psychology.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Notification createNotification(User user, Notification.NotificationType type,
                                           String title, String message) {
        return createNotification(user, type, title, message, null, null);
    }

    @Transactional
    public Notification createNotification(User user, Notification.NotificationType type,
                                           String title, String message,
                                           Long relatedEntityId, String relatedEntityType) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = notificationRepository.save(notification);

        // Отправляем уведомление через WebSocket
        sendRealTimeNotification(user, saved);

        return saved;
    }

    public List<Notification> getUserNotifications(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository.findByUser(user, pageable);
    }

    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndReadOrderByCreatedAtDesc(user, false);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndRead(user, false);
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Notification does not belong to this user");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadByUser(user);
    }

    @Transactional
    public void deleteNotification(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Notification does not belong to this user");
        }

        notificationRepository.delete(notification);
    }
    public void sendNewMessageNotification(ChatDTO.MessageResponse message) {
        // Создаем уведомление в базе данных
        // Для этого нам нужен получатель, но у нас только его ID
        // Мы не можем создать Notification без объекта User

        // Вместо этого, отправляем через WebSocket напрямую
        String destination = "/user/" + message.getReceiverId() + "/queue/notifications";

        ChatDTO.ChatNotification notification = new ChatDTO.ChatNotification();
        notification.setMessageId(message.getId());
        notification.setSenderId(message.getSenderId());
        notification.setSenderName(message.getSenderName());
        notification.setContent(message.getContent());
        notification.setSentAt(message.getSentAt());

        messagingTemplate.convertAndSend(destination, notification);
        log.info("Sent new message notification to user {}: {}", message.getReceiverId(), message.getContent());
    }

    public void sendTypingNotification(Long senderId, Long receiverId, boolean isTyping) {
        String destination = "/user/" + receiverId + "/queue/typing";

        TypingNotificationDTO notification = new TypingNotificationDTO();
        notification.setSenderId(senderId);
        notification.setTyping(isTyping);

        messagingTemplate.convertAndSend(destination, notification);
        log.debug("Sent typing notification from {} to {}: {}", senderId, receiverId, isTyping);
    }

    public void sendMessageReadNotification(Long messageId, Long readerId) {
        String destination = "/queue/messages/read/" + messageId;
        messagingTemplate.convertAndSend(destination, readerId);
        log.debug("Sent message read notification for message {} by user {}", messageId, readerId);
    }

    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime expirationDate = LocalDateTime.now().minusMonths(6); // Храним 6 месяцев
        List<Notification> allNotifications = notificationRepository.findAll();
        List<User> users = allNotifications.stream()
                .map(Notification::getUser)
                .distinct()
                .toList();

        for (User user : users) {
            notificationRepository.deleteOldNotifications(user, expirationDate);
        }
        log.info("Old notifications cleanup completed");
    }

    private void sendRealTimeNotification(User user, Notification notification) {
        String destination = "/user/" + user.getId() + "/queue/notifications";
        messagingTemplate.convertAndSend(destination, convertToDTO(notification));
        log.debug("Real-time notification sent to user {}: {}", user.getId(), notification.getTitle());
    }

    public NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setRead(notification.isRead());
        dto.setRelatedEntityId(notification.getRelatedEntityId());
        dto.setRelatedEntityType(notification.getRelatedEntityType());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }

    @lombok.Data
    public static class NotificationDTO {
        private Long id;
        private Notification.NotificationType type;
        private String title;
        private String message;
        private boolean read;
        private Long relatedEntityId;
        private String relatedEntityType;
        private LocalDateTime createdAt;
    }
}