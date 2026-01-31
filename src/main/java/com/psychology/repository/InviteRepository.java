package com.psychology.repository;

import com.psychology.model.entity.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<Invite, Long> {
    Optional<Invite> findByToken(String token);

    // Кастомный запрос с JOIN FETCH для загрузки psychologist
    @Query("SELECT i FROM Invite i JOIN FETCH i.psychologist WHERE i.token = :token")
    Optional<Invite> findByTokenWithPsychologist(@Param("token") String token);

    boolean existsByToken(String token);
    List<Invite> findByPsychologistIdAndUsedFalse(Long psychologistId);
}