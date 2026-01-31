package com.psychology.service;

import com.psychology.controller.InviteController.InviteDTO;
import com.psychology.controller.InviteController.InviteValidationResponse;
import com.psychology.model.entity.Invite;
import com.psychology.model.entity.Psychologist;
import com.psychology.repository.InviteRepository;
import com.psychology.repository.PsychologistRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InviteService {
    private final InviteRepository inviteRepository;
    private final PsychologistRepository psychologistRepository;

    private static final int INVITE_TOKEN_LENGTH = 32;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public String createInvite(Psychologist psychologist) {
        String token;
        do {
            token = RandomStringUtils.randomAlphanumeric(INVITE_TOKEN_LENGTH);
        } while (inviteRepository.existsByToken(token));

        Invite invite = new Invite();
        invite.setToken(token);
        invite.setPsychologist(psychologist);
        invite.setCreatedAt(LocalDateTime.now());
        invite.setExpiresAt(LocalDateTime.now().plusDays(7));
        invite.setUsed(false);

        inviteRepository.save(invite);
        return token;
    }

    public List<InviteDTO> getInvitesByPsychologist(Long psychologistId) {
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

    public InviteValidationResponse validateInvite(String token) {
        Invite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invite not found"));

        boolean valid = !invite.isUsed() && invite.getExpiresAt().isAfter(LocalDateTime.now());
        String psychologistName = invite.getPsychologist().getFullName();
        String expiresAt = invite.getExpiresAt().format(FORMATTER);

        return new InviteValidationResponse(valid, psychologistName, expiresAt);
    }
}