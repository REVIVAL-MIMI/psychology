package com.psychology.service;

import com.psychology.controller.InviteController.InviteDTO;
import com.psychology.controller.InviteController.InviteValidationResponse;
import com.psychology.model.entity.Invite;
import com.psychology.model.entity.Psychologist;
import com.psychology.repository.InviteRepository;
import com.psychology.repository.PsychologistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InviteService {
    private final InviteRepository inviteRepository;
    private final PsychologistRepository psychologistRepository;

    private static final int INVITE_TOKEN_LENGTH = 32;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Transactional
    public String createInvite(Psychologist psychologist) {
        log.info("Creating invite for psychologist ID: {}", psychologist.getId());

        // Загружаем психолога из базы
        Psychologist managedPsychologist = psychologistRepository.findById(psychologist.getId())
                .orElseThrow(() -> {
                    log.error("Psychologist not found with ID: {}", psychologist.getId());
                    return new RuntimeException("Psychologist not found");
                });

        log.info("Loaded psychologist: ID={}, Name={}, Verified={}",
                managedPsychologist.getId(),
                managedPsychologist.getFullName(),
                managedPsychologist.isVerified());

        String token;
        int attempts = 0;
        do {
            token = RandomStringUtils.randomAlphanumeric(INVITE_TOKEN_LENGTH);
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("Failed to generate unique token after 10 attempts");
            }
        } while (inviteRepository.existsByToken(token));

        Invite invite = new Invite();
        invite.setToken(token);
        invite.setPsychologist(managedPsychologist);
        invite.setCreatedAt(LocalDateTime.now());
        invite.setExpiresAt(LocalDateTime.now().plusDays(7));
        invite.setUsed(false);

        try {
            inviteRepository.save(invite);
            log.info("Invite created successfully. Token: {}, Psychologist ID: {}",
                    token, managedPsychologist.getId());
        } catch (Exception e) {
            log.error("Error saving invite: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save invite: " + e.getMessage());
        }

        return token;
    }

    public List<InviteDTO> getInvitesByPsychologist(Long psychologistId) {
        log.info("Getting invites for psychologist ID: {}", psychologistId);

        List<Invite> invites = inviteRepository.findByPsychologistIdAndUsedFalse(psychologistId);

        return invites.stream()
                .map(invite -> new InviteDTO(
                        invite.getToken(),
                        invite.getCreatedAt().format(FORMATTER),
                        invite.getExpiresAt().format(FORMATTER),
                        invite.isUsed()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true) // Добавляем транзакцию для чтения
    public InviteValidationResponse validateInvite(String token) {
        log.info("Validating invite token: {}", token);

        // Используем кастомный запрос для загрузки psychologist сразу
        Invite invite = inviteRepository.findByTokenWithPsychologist(token)
                .orElseThrow(() -> {
                    log.error("Invite not found for token: {}", token);
                    return new RuntimeException("Invite not found");
                });

        boolean valid = !invite.isUsed() && invite.getExpiresAt().isAfter(LocalDateTime.now());

        // Теперь psychologist должен быть загружен
        String psychologistName = invite.getPsychologist().getFullName();
        String expiresAt = invite.getExpiresAt().format(FORMATTER);

        log.info("Invite validation result: valid={}, psychologist={}", valid, psychologistName);

        return new InviteValidationResponse(valid, psychologistName, expiresAt);
    }
}