package com.psychology.service;

import com.psychology.controller.AdminController;
import com.psychology.model.entity.Psychologist;
import com.psychology.repository.PsychologistRepository;
import com.psychology.repository.ClientRepository;
import com.psychology.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final PsychologistRepository psychologistRepository;
    private final ClientRepository clientRepository;
    private final SessionRepository sessionRepository;

    public List<Psychologist> getPendingPsychologists() {
        return psychologistRepository.findByVerifiedFalse();
    }

    public List<Psychologist> getAllPsychologists() {
        return psychologistRepository.findAll();
    }

    @Transactional
    public Psychologist verifyPsychologist(Long psychologistId) {
        Psychologist psychologist = psychologistRepository.findById(psychologistId)
                .orElseThrow(() -> new RuntimeException("Psychologist not found"));

        if (psychologist.isVerified()) {
            throw new RuntimeException("Psychologist is already verified");
        }

        psychologist.setVerified(true);
        psychologist.setVerifiedAt(LocalDateTime.now());

        return psychologistRepository.save(psychologist);
    }

    @Transactional
    public void rejectPsychologist(Long psychologistId, String reason) {
        Psychologist psychologist = psychologistRepository.findById(psychologistId)
                .orElseThrow(() -> new RuntimeException("Psychologist not found"));

        if (psychologist.isVerified()) {
            throw new RuntimeException("Cannot reject already verified psychologist");
        }

        // Здесь можно добавить логику отправки email с причиной отказа
        // или сохранить причину в базе данных

        psychologistRepository.delete(psychologist);
    }

    public AdminController.AdminStats getAdminStats() {
        AdminController.AdminStats stats = new AdminController.AdminStats();

        stats.setTotalPsychologists(psychologistRepository.count());
        stats.setPendingPsychologists(psychologistRepository.countByVerifiedFalse());
        stats.setVerifiedPsychologists(psychologistRepository.countByVerifiedTrue());
        stats.setTotalClients(clientRepository.count());

        // Пример: активные сессии сегодня
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        stats.setActiveSessionsToday(sessionRepository.countByStatusAndDateTimeBetween(
                "SCHEDULED", startOfDay, endOfDay));

        return stats;
    }
}