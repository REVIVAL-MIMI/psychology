package com.psychology.service;

import com.psychology.controller.JournalController.JournalEntryRequest;
import com.psychology.controller.JournalController.JournalStats;
import com.psychology.model.entity.JournalEntry;
import com.psychology.model.entity.Client;
import com.psychology.repository.JournalEntryRepository;
import com.psychology.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final ClientRepository clientRepository;

    private static final int MAX_CONTENT_LENGTH = 5000;
    private static final int MAX_ENTRIES_PER_DAY = 10;

    @Transactional
    public JournalEntry createEntry(Client client, JournalEntryRequest request) {
        validateContent(request.getContent());

        // Проверяем лимит записей в день
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long entriesToday = journalEntryRepository.countByClientIdAndCreatedAtBetween(
                client.getId(), startOfDay, endOfDay);

        if (entriesToday >= MAX_ENTRIES_PER_DAY) {
            throw new RuntimeException("Daily limit of " + MAX_ENTRIES_PER_DAY + " entries reached");
        }

        JournalEntry entry = new JournalEntry();
        entry.setClient(client);
        entry.setContent(request.getContent());
        entry.setMood(request.getMood());
        entry.setTags(request.getTags() != null ? request.getTags() : new ArrayList<>());
        entry.setCreatedAt(LocalDateTime.now());

        return journalEntryRepository.save(entry);
    }

    public List<JournalEntry> getClientEntries(Long clientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return journalEntryRepository.findByClientId(clientId, pageable);
    }

    public JournalEntry getClientEntry(Long clientId, Long entryId) {
        JournalEntry entry = journalEntryRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Journal entry not found"));

        if (!entry.getClient().getId().equals(clientId)) {
            throw new RuntimeException("Access denied");
        }

        return entry;
    }

    @Transactional
    public JournalEntry updateEntry(Long clientId, Long entryId, JournalEntryRequest request) {
        JournalEntry entry = getClientEntry(clientId, entryId);

        if (request.getContent() != null) {
            validateContent(request.getContent());
            entry.setContent(request.getContent());
        }
        if (request.getMood() != null) {
            entry.setMood(request.getMood());
        }
        if (request.getTags() != null) {
            entry.setTags(request.getTags());
        }

        return journalEntryRepository.save(entry);
    }

    @Transactional
    public void deleteEntry(Long clientId, Long entryId) {
        JournalEntry entry = getClientEntry(clientId, entryId);
        journalEntryRepository.delete(entry);
    }

    public List<JournalEntry> getClientEntriesForPsychologist(Long psychologistId, Long clientId, int page, int size) {
        // Проверяем, что клиент принадлежит психологу
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        if (!client.getPsychologist().getId().equals(psychologistId)) {
            throw new RuntimeException("Client does not belong to this psychologist");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return journalEntryRepository.findByClientId(clientId, pageable);
    }

    public JournalEntry getClientEntryForPsychologist(Long psychologistId, Long clientId, Long entryId) {
        // Сначала проверяем доступ психолога к клиенту
        getClientEntriesForPsychologist(psychologistId, clientId, 0, 1);

        // Затем получаем запись
        return getClientEntry(clientId, entryId);
    }

    public List<JournalEntry> searchEntriesByTag(Long clientId, String tag) {
        List<JournalEntry> allEntries = journalEntryRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        return allEntries.stream()
                .filter(entry -> entry.getTags() != null && entry.getTags().contains(tag))
                .collect(Collectors.toList());
    }

    public JournalStats getJournalStats(Long clientId) {
        List<JournalEntry> entries = journalEntryRepository.findByClientIdOrderByCreatedAtDesc(clientId);

        if (entries.isEmpty()) {
            JournalStats stats = new JournalStats();
            stats.setTotalEntries(0);
            stats.setEntriesThisMonth(0);
            stats.setMostCommonMood("No entries yet");
            stats.setMostUsedTags(new ArrayList<>());
            return stats;
        }

        // Подсчет статистики
        JournalStats stats = new JournalStats();
        stats.setTotalEntries(entries.size());

        // Записи за текущий месяц
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long entriesThisMonth = entries.stream()
                .filter(e -> e.getCreatedAt().isAfter(startOfMonth))
                .count();
        stats.setEntriesThisMonth(entriesThisMonth);

        // Самое частое настроение
        Map<String, Long> moodCount = entries.stream()
                .filter(e -> e.getMood() != null && !e.getMood().trim().isEmpty())
                .collect(Collectors.groupingBy(JournalEntry::getMood, Collectors.counting()));

        String mostCommonMood = moodCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Not specified");
        stats.setMostCommonMood(mostCommonMood);

        // Самые используемые теги
        Map<String, Long> tagCount = entries.stream()
                .filter(e -> e.getTags() != null)
                .flatMap(e -> e.getTags().stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        List<String> mostUsedTags = tagCount.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        stats.setMostUsedTags(mostUsedTags);

        return stats;
    }

    @Transactional
    public void cleanupOldEntries() {
        LocalDateTime threeYearsAgo = LocalDateTime.now().minusYears(3);
        List<JournalEntry> oldEntries = journalEntryRepository.findByCreatedAtBefore(threeYearsAgo);

        if (!oldEntries.isEmpty()) {
            log.info("Cleaning up {} old journal entries", oldEntries.size());
            journalEntryRepository.deleteAll(oldEntries);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Content cannot be empty");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new RuntimeException("Content exceeds maximum length of " + MAX_CONTENT_LENGTH + " characters");
        }
    }
}