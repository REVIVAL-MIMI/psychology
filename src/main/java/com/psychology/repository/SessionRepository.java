package com.psychology.repository;

import com.psychology.model.entity.Session;
import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByPsychologistIdOrderByScheduledAtDesc(Long psychologistId);

    List<Session> findByClientIdOrderByScheduledAtDesc(Long clientId);

    List<Session> findByPsychologistIdAndScheduledAtBetween(
            Long psychologistId, LocalDateTime start, LocalDateTime end);

    List<Session> findByClientIdAndScheduledAtBetween(
            Long clientId, LocalDateTime start, LocalDateTime end);

    List<Session> findByPsychologistIdAndStatus(Long psychologistId, Session.SessionStatus status);

    List<Session> findByClientIdAndStatus(Long clientId, Session.SessionStatus status);

    @Query("SELECT COUNT(s) FROM Session s WHERE s.status = :status AND s.scheduledAt BETWEEN :start AND :end")
    long countByStatusAndDateTimeBetween(
            @Param("status") Session.SessionStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    List<Session> findByScheduledAtBetween(LocalDateTime start, LocalDateTime end);

    @Modifying
    @Transactional
    void deleteByClientId(Long clientId);
}
