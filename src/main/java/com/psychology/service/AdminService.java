package com.psychology.service;

import com.psychology.controller.AdminController;
import com.psychology.model.entity.Psychologist;
import com.psychology.repository.PsychologistRepository;
import com.psychology.repository.ClientRepository;
import com.psychology.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final PsychologistRepository psychologistRepository;
    private final ClientRepository clientRepository;
    private final SessionRepository sessionRepository;
    private final StringRedisTemplate stringRedisTemplate;

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
                com.psychology.model.entity.Session.SessionStatus.SCHEDULED, startOfDay, endOfDay));

        return stats;
    }

    public OtpInfo getLastOtp(String phone) {
        String key = "otp_admin:" + phone;
        String otp = stringRedisTemplate.opsForValue().get(key);
        Long ttlSeconds = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttlSeconds == null || ttlSeconds < 0) {
            ttlSeconds = 0L;
        }
        return new OtpInfo(otp, ttlSeconds);
    }

    public record OtpInfo(String otp, Long ttlSeconds) {}

    public List<OtpLog> getRecentOtps() {
        List<String> raw = stringRedisTemplate.opsForList().range("otp_admin_recent", 0, 49);
        if (raw == null) return List.of();

        return raw.stream()
                .map(entry -> {
                    String[] parts = entry.split("\\|", 3);
                    String phone = parts.length > 0 ? parts[0] : "";
                    String otp = parts.length > 1 ? parts[1] : "";
                    long ts = parts.length > 2 ? Long.parseLong(parts[2]) : 0L;
                    return new OtpLog(phone, otp, ts);
                })
                .collect(Collectors.toList());
    }

    public record OtpLog(String phone, String otp, long timestampMs) {}
}
