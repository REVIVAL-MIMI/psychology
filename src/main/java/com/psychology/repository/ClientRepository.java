package com.psychology.repository;

import com.psychology.model.entity.Client;
import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByPsychologist(Psychologist psychologist);
    List<Client> findByPsychologistId(Long psychologistId);

    @Query("SELECT DISTINCT c FROM Client c WHERE c.psychologist.id = :psychologistId AND EXISTS (" +
            "SELECT s.id FROM Session s WHERE s.client.id = c.id AND s.scheduledAt BETWEEN :start AND :end AND s.status IN :statuses)")
    List<Client> findActiveByPsychologistIdAndDateRange(
            @Param("psychologistId") Long psychologistId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<Session.SessionStatus> statuses
    );
}
