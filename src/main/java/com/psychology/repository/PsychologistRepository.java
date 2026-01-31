package com.psychology.repository;

import com.psychology.model.entity.Psychologist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PsychologistRepository extends JpaRepository<Psychologist, Long> {
    List<Psychologist> findByVerified(boolean verified);
}