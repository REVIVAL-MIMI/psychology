package com.psychology.config;

import com.psychology.model.entity.*;
import com.psychology.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DevSeedData implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PsychologistRepository psychologistRepository;
    private final ClientRepository clientRepository;
    private final SessionRepository sessionRepository;
    private final RecommendationRepository recommendationRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String psychologistPhone = "+79990000001";
        String clientPhone = "+79990000002";

        if (userRepository.existsByPhone(psychologistPhone) || userRepository.existsByPhone(clientPhone)) {
            log.info("Seed users already exist, skipping demo data.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        Psychologist psychologist = new Psychologist();
        psychologist.setPhone(psychologistPhone);
        psychologist.setRole(UserRole.ROLE_PSYCHOLOGIST);
        psychologist.setFullName("Алексей Романов");
        psychologist.setEmail("psy@example.com");
        psychologist.setEducation("МГУ, клиническая психология");
        psychologist.setSpecialization("Тревожность, стресс, отношения");
        psychologist.setDescription("Тихая работа с фокусом на ритм, устойчивость и бережные изменения.");
        psychologist.setVerified(true);
        psychologist.setVerifiedAt(now.minusDays(30));
        psychologist = psychologistRepository.save(psychologist);

        Client client = new Client();
        client.setPhone(clientPhone);
        client.setRole(UserRole.ROLE_CLIENT);
        client.setFullName("Мария Кузнецова");
        client.setAge(28);
        client.setPsychologist(psychologist);
        client.setLinkedAt(now.minusDays(20));
        client = clientRepository.save(client);

        Session upcoming = new Session();
        upcoming.setPsychologist(psychologist);
        upcoming.setClient(client);
        upcoming.setScheduledAt(now.plusDays(2).withHour(10).withMinute(30));
        upcoming.setDurationMinutes(50);
        upcoming.setDescription("Фокус на тревоге перед публичными выступлениями");
        upcoming.setStatus(Session.SessionStatus.SCHEDULED);

        Session confirmed = new Session();
        confirmed.setPsychologist(psychologist);
        confirmed.setClient(client);
        confirmed.setScheduledAt(now.plusDays(6).withHour(18).withMinute(0));
        confirmed.setDurationMinutes(50);
        confirmed.setDescription("Укрепление границ и ритма недели");
        confirmed.setStatus(Session.SessionStatus.CONFIRMED);

        Session completed = new Session();
        completed.setPsychologist(psychologist);
        completed.setClient(client);
        completed.setScheduledAt(now.minusDays(3).withHour(12).withMinute(0));
        completed.setDurationMinutes(50);
        completed.setDescription("Работа с дневником тревоги");
        completed.setStatus(Session.SessionStatus.COMPLETED);

        Session cancelled = new Session();
        cancelled.setPsychologist(psychologist);
        cancelled.setClient(client);
        cancelled.setScheduledAt(now.minusDays(10).withHour(9).withMinute(0));
        cancelled.setDurationMinutes(50);
        cancelled.setDescription("Отменён по просьбе клиента");
        cancelled.setStatus(Session.SessionStatus.CANCELLED);

        sessionRepository.saveAll(List.of(upcoming, confirmed, completed, cancelled));

        Recommendation rec1 = new Recommendation();
        rec1.setPsychologist(psychologist);
        rec1.setClient(client);
        rec1.setTitle("Ритуал утренней ясности");
        rec1.setContent("10 минут дыхания 4‑6 и короткая запись: «что важно сегодня».");
        rec1.setDeadline(now.plusDays(5));
        rec1.setPriority(4);
        rec1.setCategories(List.of("дыхание", "ритуалы"));

        Recommendation rec2 = new Recommendation();
        rec2.setPsychologist(psychologist);
        rec2.setClient(client);
        rec2.setTitle("Дневник спокойствия");
        rec2.setContent("Вечером 3 наблюдения: факт, реакция, новая интерпретация.");
        rec2.setDeadline(now.plusDays(10));
        rec2.setPriority(3);
        rec2.setCategories(List.of("дневник", "рефлексия"));

        Recommendation rec3 = new Recommendation();
        rec3.setPsychologist(psychologist);
        rec3.setClient(client);
        rec3.setTitle("Тренировка границ");
        rec3.setContent("Отметить 2 ситуации в неделю, где удалось сказать «нет».");
        rec3.setDeadline(now.minusDays(2));
        rec3.setPriority(2);
        rec3.setCategories(List.of("границы"));
        rec3.setCompleted(true);
        rec3.setCompletedByClient(true);
        rec3.setCompletedAt(now.minusDays(1));

        recommendationRepository.saveAll(List.of(rec1, rec2, rec3));

        JournalEntry entry1 = new JournalEntry();
        entry1.setClient(client);
        entry1.setContent("Сегодня удалось спокойно проговорить ожидания, без спешки и напряжения.");
        entry1.setMood("спокойно");
        entry1.setTags(List.of("границы", "работа"));
        entry1.setCreatedAt(now.minusDays(4));

        JournalEntry entry2 = new JournalEntry();
        entry2.setClient(client);
        entry2.setContent("С утра тревога, помогло дыхание и короткая прогулка.");
        entry2.setMood("тревожно");
        entry2.setTags(List.of("дыхание", "прогулка"));
        entry2.setCreatedAt(now.minusDays(2));

        journalEntryRepository.saveAll(List.of(entry1, entry2));

        Message m1 = new Message();
        m1.setSender(psychologist);
        m1.setReceiver(client);
        m1.setContent("Мария, как ощущается неделя? Есть ли один момент, где было спокойнее?");
        m1.setRead(true);
        m1.setSentAt(now.minusDays(3).withHour(11).withMinute(15));

        Message m2 = new Message();
        m2.setSender(client);
        m2.setReceiver(psychologist);
        m2.setContent("Да, утром в понедельник получилось начать без спешки.");
        m2.setRead(true);
        m2.setSentAt(now.minusDays(3).withHour(11).withMinute(18));

        Message m3 = new Message();
        m3.setSender(psychologist);
        m3.setReceiver(client);
        m3.setContent("Отлично. Давай закрепим это коротким ритуалом из 3 шагов.");
        m3.setRead(true);
        m3.setSentAt(now.minusDays(3).withHour(11).withMinute(22));

        Message m4 = new Message();
        m4.setSender(client);
        m4.setReceiver(psychologist);
        m4.setContent("Получилось. Записала в дневник.");
        m4.setRead(true);
        m4.setSentAt(now.minusDays(2).withHour(20).withMinute(5));

        Message m5 = new Message();
        m5.setSender(psychologist);
        m5.setReceiver(client);
        m5.setContent("📹 Видеозвонок: 12 минут. Итоги — отправлю в рекомендациях.");
        m5.setRead(true);
        m5.setSentAt(now.minusDays(1).withHour(18).withMinute(30));

        Message m6 = new Message();
        m6.setSender(psychologist);
        m6.setReceiver(client);
        m6.setContent("Напомню: завтра сеанс в 10:30. Если нужно перенести — просто напишите.");
        m6.setRead(false);
        m6.setSentAt(now.minusHours(2));

        messageRepository.saveAll(List.of(m1, m2, m3, m4, m5, m6));

        log.info("Demo data created: psychologist={}, client={}", psychologistPhone, clientPhone);
    }
}
