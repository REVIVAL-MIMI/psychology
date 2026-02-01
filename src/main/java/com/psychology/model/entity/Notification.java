package com.psychology.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "related_entity_id")
    private Long relatedEntityId; // ID связанной сущности (сеанса, сообщения, рекомендации)

    @Column(name = "related_entity_type")
    private String relatedEntityType; // Тип связанной сущности: SESSION, MESSAGE, RECOMMENDATION, etc.

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum NotificationType {
        // Чат
        NEW_MESSAGE,

        // Сеансы
        SESSION_REMINDER_24H,
        SESSION_REMINDER_1H,
        SESSION_CREATED,
        SESSION_UPDATED,
        SESSION_CANCELLED,
        SESSION_CONFIRMED,

        // Рекомендации
        NEW_RECOMMENDATION,
        RECOMMENDATION_UPDATED,
        RECOMMENDATION_OVERDUE,

        // Дневник
        JOURNAL_REMINDER, // напоминание о заполнении дневника

        // Системные
        SYSTEM_ANNOUNCEMENT,

        // Прочее
        INFO
    }
}