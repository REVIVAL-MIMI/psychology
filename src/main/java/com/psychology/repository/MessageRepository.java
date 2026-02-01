package com.psychology.repository;

import com.psychology.model.entity.Message;
import com.psychology.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender.id = :user1Id AND m.receiver.id = :user2Id) OR " +
            "(m.sender.id = :user2Id AND m.receiver.id = :user1Id) " +
            "ORDER BY m.sentAt ASC")
    List<Message> findConversation(@Param("user1Id") Long user1Id,
                                   @Param("user2Id") Long user2Id);

    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender.id = :userId OR m.receiver.id = :userId) AND " +
            "m.sentAt > :since " +
            "ORDER BY m.sentAt DESC")
    List<Message> findRecentMessages(@Param("userId") Long userId,
                                     @Param("since") LocalDateTime since);

    @Query("SELECT m FROM Message m WHERE " +
            "m.receiver.id = :userId AND m.read = false")
    List<Message> findUnreadMessages(@Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender.id = :userId OR m.receiver.id = :userId) " +
            "ORDER BY m.sentAt DESC")
    List<Message> findAllUserMessages(@Param("userId") Long userId);

    long countByReceiverIdAndReadFalse(Long receiverId);
}