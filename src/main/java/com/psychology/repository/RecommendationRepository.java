package com.psychology.repository;

import com.psychology.model.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByClientId(Long clientId);

    List<Recommendation> findByPsychologistId(Long psychologistId);

    List<Recommendation> findByClientIdAndCompleted(Long clientId, boolean completed);

    @Query("SELECT r FROM Recommendation r WHERE r.client.psychologist.id = :psychologistId AND r.completed = false AND r.deadline < :now")
    List<Recommendation> findOverdueByPsychologistId(@Param("psychologistId") Long psychologistId, @Param("now") LocalDateTime now);

    @Query("SELECT r FROM Recommendation r WHERE r.completed = false AND r.deadline < :deadline")
    List<Recommendation> findOverdueRecommendations(@Param("deadline") LocalDateTime deadline);

    @Query("SELECT r FROM Recommendation r WHERE r.client.id = :clientId AND r.completed = false ORDER BY r.priority DESC, r.deadline ASC")
    List<Recommendation> findActiveByClientId(@Param("clientId") Long clientId);

    long countByClientIdAndCompleted(Long clientId, boolean completed);

    @Modifying
    @Transactional
    void deleteByClientId(Long clientId);
}
