package com.psychology.repository;

import com.psychology.model.entity.Session;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = {"client", "psychologist"})
    @Query("SELECT s FROM Session s WHERE s.psychologist.id = :psychologistId ORDER BY s.scheduledAt DESC")
    List<Session> findByPsychologistIdOrderByScheduledAtDescWithParticipants(@Param("psychologistId") Long psychologistId);

    @EntityGraph(attributePaths = {"client", "psychologist"})
    @Query("SELECT s FROM Session s WHERE s.client.id = :clientId ORDER BY s.scheduledAt DESC")
    List<Session> findByClientIdOrderByScheduledAtDescWithParticipants(@Param("clientId") Long clientId);

    List<Session> findByPsychologistIdAndScheduledAtBetween(
            Long psychologistId, LocalDateTime start, LocalDateTime end);

    List<Session> findByPsychologistIdAndScheduledAtBetweenAndStatusIn(
            Long psychologistId, LocalDateTime start, LocalDateTime end, List<Session.SessionStatus> statuses);

    List<Session> findByClientIdAndScheduledAtBetween(
            Long clientId, LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"client", "psychologist"})
    @Query("SELECT s FROM Session s WHERE s.psychologist.id = :psychologistId AND s.scheduledAt BETWEEN :start AND :end ORDER BY s.scheduledAt DESC")
    List<Session> findByPsychologistIdAndScheduledAtBetweenWithParticipants(
            @Param("psychologistId") Long psychologistId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @EntityGraph(attributePaths = {"client", "psychologist"})
    @Query("SELECT s FROM Session s WHERE s.client.id = :clientId AND s.scheduledAt BETWEEN :start AND :end ORDER BY s.scheduledAt DESC")
    List<Session> findByClientIdAndScheduledAtBetweenWithParticipants(
            @Param("clientId") Long clientId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<Session> findByPsychologistIdAndStatus(Long psychologistId, Session.SessionStatus status);

    List<Session> findByClientIdAndStatus(Long clientId, Session.SessionStatus status);

    long countByPsychologistIdAndScheduledAtBetweenAndStatusIn(
            Long psychologistId,
            LocalDateTime start,
            LocalDateTime end,
            List<Session.SessionStatus> statuses
    );

    @Query("SELECT COUNT(DISTINCT s.client.id) FROM Session s WHERE s.psychologist.id = :psychologistId " +
            "AND s.scheduledAt BETWEEN :start AND :end AND s.status IN :statuses")
    long countDistinctClientIdsByPsychologistIdAndScheduledAtBetweenAndStatusIn(
            @Param("psychologistId") Long psychologistId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<Session.SessionStatus> statuses
    );

    @EntityGraph(attributePaths = {"client", "psychologist"})
    @Query("SELECT s FROM Session s WHERE s.psychologist.id = :psychologistId " +
            "AND s.scheduledAt BETWEEN :start AND :end AND s.status IN :statuses ORDER BY s.scheduledAt ASC")
    List<Session> findByPsychologistIdAndScheduledAtBetweenAndStatusInWithParticipants(
            @Param("psychologistId") Long psychologistId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<Session.SessionStatus> statuses
    );

    @EntityGraph(attributePaths = {"client", "psychologist"})
    @Query("SELECT s FROM Session s WHERE s.client.id = :clientId " +
            "AND s.scheduledAt BETWEEN :start AND :end AND s.status IN :statuses ORDER BY s.scheduledAt ASC")
    List<Session> findByClientIdAndScheduledAtBetweenAndStatusInWithParticipants(
            @Param("clientId") Long clientId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<Session.SessionStatus> statuses
    );

    @Query("SELECT s FROM Session s JOIN FETCH s.client WHERE s.psychologist.id = :psychologistId " +
            "AND s.scheduledAt BETWEEN :start AND :end AND s.status IN :statuses")
    List<Session> findByPsychologistIdAndScheduledAtBetweenAndStatusInWithClient(
            @Param("psychologistId") Long psychologistId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<Session.SessionStatus> statuses
    );

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
