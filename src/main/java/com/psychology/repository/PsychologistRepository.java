package com.psychology.repository;

import com.psychology.model.entity.Psychologist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PsychologistRepository extends JpaRepository<Psychologist, Long> {
    List<Psychologist> findByVerified(boolean verified);

    Optional<Psychologist> findByPhone(String phone);

    default List<Psychologist> findByVerifiedFalse() {
        return findByVerified(false);
    }

    default List<Psychologist> findByVerifiedTrue() {
        return findByVerified(true);
    }

    @Query("SELECT COUNT(p) FROM Psychologist p WHERE p.verified = true")
    long countByVerifiedTrue();

    @Query("SELECT COUNT(p) FROM Psychologist p WHERE p.verified = false")
    long countByVerifiedFalse();
}