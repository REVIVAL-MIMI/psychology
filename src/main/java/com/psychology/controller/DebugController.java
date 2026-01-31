package com.psychology.controller;

import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.Invite;
import com.psychology.repository.PsychologistRepository;
import com.psychology.repository.InviteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/debug")  // ✅ Путь начинается с /api/v1/debug
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final PsychologistRepository psychologistRepository;
    private final InviteRepository inviteRepository;

    @GetMapping("/psychologists")
    public ResponseEntity<List<Psychologist>> getAllPsychologists() {
        log.info("Getting all psychologists");
        List<Psychologist> psychologists = psychologistRepository.findAll();
        return ResponseEntity.ok(psychologists);
    }

    @GetMapping("/invites")
    public ResponseEntity<List<Invite>> getAllInvites() {
        log.info("Getting all invites");
        List<Invite> invites = inviteRepository.findAll();
        return ResponseEntity.ok(invites);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}