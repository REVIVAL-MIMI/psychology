package com.psychology.repository;

import com.psychology.model.entity.Notification;
import com.psychology.model.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndReadOrderByCreatedAtDesc(User user, boolean read);

    List<Notification> findByUser(User user, Pageable pageable);

    long countByUserAndRead(User user, boolean read);

    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findByUserSince(@Param("user") User user, @Param("since") LocalDateTime since);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user = :user AND n.read = false")
    void markAllAsReadByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.createdAt < :expirationDate")
    void deleteOldNotifications(@Param("user") User user, @Param("expirationDate") LocalDateTime expirationDate);

    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.type = :type AND n.relatedEntityId = :relatedEntityId")
    List<Notification> findByUserAndTypeAndRelatedEntityId(@Param("user") User user,
                                                           @Param("type") Notification.NotificationType type,
                                                           @Param("relatedEntityId") Long relatedEntityId);
}