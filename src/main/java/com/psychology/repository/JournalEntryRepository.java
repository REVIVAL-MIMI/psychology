package com.psychology.repository;

import com.psychology.model.entity.JournalEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    List<JournalEntry> findByClientIdOrderByCreatedAtDesc(Long clientId);

    @Query("SELECT je FROM JournalEntry je WHERE je.client.id = :clientId ORDER BY je.createdAt DESC")
    List<JournalEntry> findByClientId(@Param("clientId") Long clientId, Pageable pageable);

    List<JournalEntry> findByClientId(@Param("clientId") Long clientId);


    long countByClientIdAndCreatedAtBetween(Long clientId, LocalDateTime start, LocalDateTime end);

    List<JournalEntry> findByCreatedAtBefore(LocalDateTime date);

    @Query("SELECT DISTINCT je FROM JournalEntry je JOIN je.tags tag WHERE je.client.id = :clientId AND tag = :tag")
    List<JournalEntry> findByClientIdAndTag(@Param("clientId") Long clientId, @Param("tag") String tag);

    @Modifying
    @Transactional
    void deleteByClientId(Long clientId);
}
