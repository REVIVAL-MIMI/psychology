package com.psychology.service;

import com.psychology.model.entity.Session;
import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.Client;
import com.psychology.repository.SessionRepository;
import com.psychology.repository.ClientRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ClientRepository clientRepository;

    @Transactional
    public Session createSession(Psychologist psychologist, SessionRequest request) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // Проверяем, что клиент принадлежит психологу
        if (!client.getPsychologist().getId().equals(psychologist.getId())) {
            throw new RuntimeException("Client does not belong to this psychologist");
        }

        // Проверяем, что время сеанса в будущем
        if (request.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Session time must be in the future");
        }

        Session session = new Session();
        session.setPsychologist(psychologist);
        session.setClient(client);
        session.setScheduledAt(request.getScheduledAt());
        session.setDurationMinutes(request.getDurationMinutes());
        session.setDescription(request.getDescription());
        session.setStatus(Session.SessionStatus.SCHEDULED);

        return sessionRepository.save(session);
    }

    @Transactional
    public Session updateSession(Long sessionId, Psychologist psychologist, SessionUpdateRequest request) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Проверяем, что сеанс принадлежит психологу
        if (!session.getPsychologist().getId().equals(psychologist.getId())) {
            throw new RuntimeException("Session does not belong to this psychologist");
        }

        if (request.getScheduledAt() != null) {
            // Проверяем, что время сеанса в будущем
            if (request.getScheduledAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Session time must be in the future");
            }
            session.setScheduledAt(request.getScheduledAt());
        }
        if (request.getDurationMinutes() != null) {
            session.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getDescription() != null) {
            session.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            session.setStatus(request.getStatus());
        }

        return sessionRepository.save(session);
    }

    public List<Session> getPsychologistSessions(Psychologist psychologist, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null) {
            return sessionRepository.findByPsychologistIdAndScheduledAtBetween(
                    psychologist.getId(), from, to);
        }
        return sessionRepository.findByPsychologistIdOrderByScheduledAtDesc(psychologist.getId());
    }

    public List<Session> getClientSessions(Client client, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null) {
            return sessionRepository.findByClientIdAndScheduledAtBetween(client.getId(), from, to);
        }
        return sessionRepository.findByClientIdOrderByScheduledAtDesc(client.getId());
    }

    @Transactional
    public Session cancelSession(Long sessionId, String userType, Object user) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Проверяем, что сеанс можно отменить
        if (session.getStatus() != Session.SessionStatus.SCHEDULED &&
                session.getStatus() != Session.SessionStatus.CONFIRMED) {
            throw new RuntimeException("Cannot cancel session in status: " + session.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionTime = session.getScheduledAt();

        if (userType.equals("PSYCHOLOGIST")) {
            Psychologist psychologist = (Psychologist) user;
            if (!session.getPsychologist().getId().equals(psychologist.getId())) {
                throw new RuntimeException("Session does not belong to this psychologist");
            }
            // Психолог может отменить в любое время
            session.setStatus(Session.SessionStatus.CANCELLED);
        } else if (userType.equals("CLIENT")) {
            Client client = (Client) user;
            if (!session.getClient().getId().equals(client.getId())) {
                throw new RuntimeException("Session does not belong to this client");
            }
            // Клиент может отменить не позднее чем за 12 часов до начала
            if (sessionTime.minusHours(12).isBefore(now)) {
                throw new RuntimeException("Cannot cancel session less than 12 hours before start");
            }
            session.setStatus(Session.SessionStatus.CANCELLED);
        } else {
            throw new RuntimeException("Invalid user type");
        }

        return sessionRepository.save(session);
    }

    @Data
    public static class SessionRequest {
        private Long clientId;
        private LocalDateTime scheduledAt;
        private Integer durationMinutes = 50;
        private String description;
    }

    @Data
    public static class SessionUpdateRequest {
        private LocalDateTime scheduledAt;
        private Integer durationMinutes;
        private String description;
        private Session.SessionStatus status;
    }
}