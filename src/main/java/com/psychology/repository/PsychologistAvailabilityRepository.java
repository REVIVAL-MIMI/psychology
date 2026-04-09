package com.psychology.repository;

import com.psychology.model.entity.PsychologistAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PsychologistAvailabilityRepository extends JpaRepository<PsychologistAvailability, Long> {
    List<PsychologistAvailability> findByPsychologistIdAndWorkDateBetween(Long psychologistId, LocalDate from, LocalDate to);

    Optional<PsychologistAvailability> findByPsychologistIdAndWorkDate(Long psychologistId, LocalDate workDate);
}
