package com.psychology.controller;

import com.psychology.model.entity.Client;
import com.psychology.model.entity.JournalEntry;
import com.psychology.model.entity.Psychologist;
import com.psychology.service.JournalService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/journal")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    // Клиент создает запись в дневнике
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> createEntry(
            @AuthenticationPrincipal Client client,
            @RequestBody JournalEntryRequest request) {
        try {
            JournalEntry entry = journalService.createEntry(client, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(entry);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Клиент получает свои записи
    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<JournalEntry>> getMyEntries(
            @AuthenticationPrincipal Client client,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        List<JournalEntry> entries = journalService.getClientEntries(client.getId(), pageNumber, pageSize);
        return ResponseEntity.ok(entries);
    }

    // Клиент получает запись по ID
    @GetMapping("/{entryId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getEntry(
            @AuthenticationPrincipal Client client,
            @PathVariable Long entryId) {
        try {
            JournalEntry entry = journalService.getClientEntry(client.getId(), entryId);
            return ResponseEntity.ok(entry);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Клиент обновляет запись
    @PutMapping("/{entryId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> updateEntry(
            @AuthenticationPrincipal Client client,
            @PathVariable Long entryId,
            @RequestBody JournalEntryRequest request) {
        try {
            JournalEntry entry = journalService.updateEntry(client.getId(), entryId, request);
            return ResponseEntity.ok(entry);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Клиент удаляет запись
    @DeleteMapping("/{entryId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> deleteEntry(
            @AuthenticationPrincipal Client client,
            @PathVariable Long entryId) {
        try {
            journalService.deleteEntry(client.getId(), entryId);
            return ResponseEntity.ok(new ApiResponse("Entry deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Психолог получает записи своего клиента
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<?> getClientEntries(
            @AuthenticationPrincipal Psychologist psychologist,
            @PathVariable Long clientId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        try {
            int pageNumber = page != null ? page : 0;
            int pageSize = size != null ? size : 20;

            List<JournalEntry> entries = journalService.getClientEntriesForPsychologist(psychologist.getId(), clientId, pageNumber, pageSize);
            return ResponseEntity.ok(entries);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Психолог получает запись клиента по ID
    @GetMapping("/client/{clientId}/entry/{entryId}")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public ResponseEntity<?> getClientEntry(
            @AuthenticationPrincipal Psychologist psychologist,
            @PathVariable Long clientId,
            @PathVariable Long entryId) {
        try {
            JournalEntry entry = journalService.getClientEntryForPsychologist(psychologist.getId(), clientId, entryId);
            return ResponseEntity.ok(entry);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Поиск записей по тегам
    @GetMapping("/search")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> searchEntries(
            @AuthenticationPrincipal Client client,
            @RequestParam String tag) {
        try {
            List<JournalEntry> entries = journalService.searchEntriesByTag(client.getId(), tag);
            return ResponseEntity.ok(entries);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    // Статистика по дневнику
    @GetMapping("/stats")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<?> getJournalStats(@AuthenticationPrincipal Client client) {
        try {
            JournalStats stats = journalService.getJournalStats(client.getId());
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(e.getMessage()));
        }
    }

    @Data
    public static class JournalEntryRequest {
        private String content;
        private String mood;
        private List<String> tags;
    }

    @Data
    public static class ApiResponse {
        private String message;
        private LocalDateTime timestamp;

        public ApiResponse(String message) {
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }
    }

    @Data
    public static class JournalStats {
        private long totalEntries;
        private long entriesThisMonth;
        private String mostCommonMood;
        private List<String> mostUsedTags;
    }
}