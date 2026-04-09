package com.psychology.service;

import com.psychology.model.entity.Session;
import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.Client;
import com.psychology.model.entity.PsychologistAvailability;
import com.psychology.repository.SessionRepository;
import com.psychology.repository.ClientRepository;
import com.psychology.repository.PsychologistAvailabilityRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final int DEFAULT_WORK_START_HOUR = 9;
    private static final int DEFAULT_WORK_END_HOUR = 20;
    private static final int DEFAULT_SLOT_DURATION = 50;
    private static final int DEFAULT_DAYS_AHEAD = 14;
    private static final Locale RU_LOCALE = Locale.forLanguageTag("ru");
    private static final List<Session.SessionStatus> ACTIVE_BOOKING_STATUSES = List.of(
            Session.SessionStatus.SCHEDULED,
            Session.SessionStatus.CONFIRMED,
            Session.SessionStatus.IN_PROGRESS,
            Session.SessionStatus.RESCHEDULED
    );

    private final SessionRepository sessionRepository;
    private final ClientRepository clientRepository;
    private final PsychologistAvailabilityRepository availabilityRepository;

    @Transactional
    public Session createSession(Psychologist psychologist, SessionRequest request) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // Проверяем, что клиент принадлежит психологу
        if (!client.getPsychologist().getId().equals(psychologist.getId())) {
            throw new RuntimeException("Client does not belong to this psychologist");
        }

        int duration = resolveDuration(request.getDurationMinutes());
        validateBookableSlot(psychologist.getId(), request.getScheduledAt(), duration, null, false);

        Session session = new Session();
        session.setPsychologist(psychologist);
        session.setClient(client);
        session.setScheduledAt(request.getScheduledAt());
        session.setDurationMinutes(duration);
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

        LocalDateTime targetTime = request.getScheduledAt() != null
                ? request.getScheduledAt()
                : session.getScheduledAt();
        int targetDuration = request.getDurationMinutes() != null
                ? resolveDuration(request.getDurationMinutes())
                : resolveDuration(session.getDurationMinutes());

        if (request.getScheduledAt() != null) {
            validateBookableSlot(psychologist.getId(), targetTime, targetDuration, session.getId(), false);
        }
        if (request.getDurationMinutes() != null) {
            validateBookableSlot(psychologist.getId(), targetTime, targetDuration, session.getId(), false);
            session.setDurationMinutes(targetDuration);
        }
        if (request.getDescription() != null) {
            session.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            session.setStatus(request.getStatus());
        }
        if (request.getScheduledAt() != null) {
            session.setScheduledAt(request.getScheduledAt());
        }

        return sessionRepository.save(session);
    }

    public List<Session> getPsychologistSessions(Psychologist psychologist, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null) {
            return sessionRepository.findByPsychologistIdAndScheduledAtBetweenWithParticipants(
                    psychologist.getId(), from, to);
        }
        return sessionRepository.findByPsychologistIdOrderByScheduledAtDescWithParticipants(psychologist.getId());
    }

    public List<Session> getClientSessions(Client client, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null) {
            return sessionRepository.findByClientIdAndScheduledAtBetweenWithParticipants(client.getId(), from, to);
        }
        return sessionRepository.findByClientIdOrderByScheduledAtDescWithParticipants(client.getId());
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

    public List<AvailableDaySlots> getAvailableSlotsForClient(Client client, Integer daysAheadRaw) {
        int daysAhead = normalizeDays(daysAheadRaw);
        Long psychologistId = client.getPsychologist().getId();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(daysAhead - 1L);
        LocalDateTime now = LocalDateTime.now();

        Map<LocalDate, DayRule> ruleByDate = loadRules(psychologistId, startDate, endDate);
        Map<LocalDate, List<Session>> sessionsByDate = loadSessionsByDate(psychologistId, now, endDate.plusDays(1).atStartOfDay());

        List<AvailableDaySlots> result = new ArrayList<>();
        for (int i = 0; i < daysAhead; i++) {
            LocalDate date = startDate.plusDays(i);
            DayRule rule = resolveRule(ruleByDate, date);
            List<Session> daySessions = sessionsByDate.getOrDefault(date, List.of());
            List<LocalDateTime> slots = new ArrayList<>();

            if (rule.working()) {
                for (int hour = rule.startHour(); hour < rule.endHour(); hour++) {
                    LocalDateTime slotStart = LocalDateTime.of(date, LocalTime.of(hour, 0));
                    if (slotStart.isBefore(now)) {
                        continue;
                    }
                    LocalDateTime slotEnd = slotStart.plusMinutes(DEFAULT_SLOT_DURATION);
                    if (hasOverlap(daySessions, slotStart, slotEnd, null)) {
                        continue;
                    }
                    slots.add(slotStart);
                }
            }

            AvailableDaySlots day = new AvailableDaySlots();
            day.setDate(date);
            day.setDayOfWeek(toDayLabel(date.getDayOfWeek()));
            day.setWorking(rule.working());
            day.setWorkStartHour(rule.startHour());
            day.setWorkEndHour(rule.endHour());
            day.setAvailableSlots(slots);
            result.add(day);
        }
        return result;
    }

    @Transactional
    public Session bookSessionByClient(Client client, ClientBookingRequest request) {
        Psychologist psychologist = client.getPsychologist();
        int duration = resolveDuration(request.getDurationMinutes());
        validateBookableSlot(psychologist.getId(), request.getScheduledAt(), duration, null, true);

        Session session = new Session();
        session.setPsychologist(psychologist);
        session.setClient(client);
        session.setScheduledAt(request.getScheduledAt());
        session.setDurationMinutes(duration);
        session.setDescription(request.getDescription());
        session.setStatus(Session.SessionStatus.SCHEDULED);
        return sessionRepository.save(session);
    }

    public List<PsychologistScheduleDay> getPsychologistSchedule(Psychologist psychologist, Integer daysAheadRaw) {
        int daysAhead = normalizeDays(daysAheadRaw);
        Long psychologistId = psychologist.getId();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(daysAhead - 1L);

        Map<LocalDate, DayRule> ruleByDate = loadRules(psychologistId, startDate, endDate);
        Map<LocalDate, List<Session>> sessionsByDate = loadScheduleSessionsByDate(
                psychologistId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        );

        List<PsychologistScheduleDay> result = new ArrayList<>();
        for (int i = 0; i < daysAhead; i++) {
            LocalDate date = startDate.plusDays(i);
            DayRule rule = resolveRule(ruleByDate, date);
            List<ScheduleBookingItem> bookings = sessionsByDate.getOrDefault(date, List.of())
                    .stream()
                    .sorted(Comparator.comparing(Session::getScheduledAt))
                    .map(this::toBookingItem)
                    .collect(Collectors.toList());

            PsychologistScheduleDay day = new PsychologistScheduleDay();
            day.setDate(date);
            day.setDayOfWeek(toDayLabel(date.getDayOfWeek()));
            day.setWorking(rule.working());
            day.setWorkStartHour(rule.startHour());
            day.setWorkEndHour(rule.endHour());
            day.setBookings(bookings);
            result.add(day);
        }
        return result;
    }

    @Transactional
    public List<PsychologistScheduleDay> updatePsychologistAvailability(
            Psychologist psychologist,
            UpdateAvailabilityRequest request,
            Integer daysAheadRaw
    ) {
        if (request == null || request.getDays() == null || request.getDays().isEmpty()) {
            throw new RuntimeException("Availability days are required");
        }

        for (AvailabilityDayUpdate dayUpdate : request.getDays()) {
            if (dayUpdate.getDate() == null) {
                throw new RuntimeException("Date is required");
            }

            boolean working = Boolean.TRUE.equals(dayUpdate.getWorking());
            int startHour = dayUpdate.getWorkStartHour() != null ? dayUpdate.getWorkStartHour() : DEFAULT_WORK_START_HOUR;
            int endHour = dayUpdate.getWorkEndHour() != null ? dayUpdate.getWorkEndHour() : DEFAULT_WORK_END_HOUR;

            if (working) {
                validateHours(startHour, endHour);
            }

            PsychologistAvailability availability = availabilityRepository
                    .findByPsychologistIdAndWorkDate(psychologist.getId(), dayUpdate.getDate())
                    .orElseGet(PsychologistAvailability::new);

            availability.setPsychologist(psychologist);
            availability.setWorkDate(dayUpdate.getDate());
            availability.setWorking(working);
            availability.setWorkStartHour(working ? startHour : null);
            availability.setWorkEndHour(working ? endHour : null);
            availabilityRepository.save(availability);
        }

        return getPsychologistSchedule(psychologist, daysAheadRaw);
    }

    private void validateBookableSlot(
            Long psychologistId,
            LocalDateTime scheduledAt,
            int durationMinutes,
            Long excludeSessionId,
            boolean enforceFullHour
    ) {
        if (scheduledAt == null) {
            throw new RuntimeException("Scheduled time is required");
        }
        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Session time must be in the future");
        }
        if (enforceFullHour && (scheduledAt.getMinute() != 0 || scheduledAt.getSecond() != 0 || scheduledAt.getNano() != 0)) {
            throw new RuntimeException("Employee booking must start at a full hour");
        }

        DayRule rule = resolveRule(loadRules(psychologistId, scheduledAt.toLocalDate(), scheduledAt.toLocalDate()), scheduledAt.toLocalDate());
        if (!rule.working()) {
            throw new RuntimeException("Selected day is marked as non-working");
        }

        LocalDateTime slotEnd = scheduledAt.plusMinutes(durationMinutes);
        if (!slotEnd.toLocalDate().equals(scheduledAt.toLocalDate())) {
            throw new RuntimeException("Session must be within one day");
        }
        LocalTime startTime = scheduledAt.toLocalTime();
        LocalTime endTime = slotEnd.toLocalTime();
        if (startTime.isBefore(LocalTime.of(rule.startHour(), 0)) || endTime.isAfter(LocalTime.of(rule.endHour(), 0))) {
            throw new RuntimeException("Selected time is outside psychologist working hours");
        }

        LocalDateTime dayStart = scheduledAt.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = scheduledAt.toLocalDate().plusDays(1).atStartOfDay();
        List<Session> daySessions = sessionRepository.findByPsychologistIdAndScheduledAtBetweenAndStatusIn(
                psychologistId, dayStart, dayEnd, ACTIVE_BOOKING_STATUSES
        );
        if (hasOverlap(daySessions, scheduledAt, slotEnd, excludeSessionId)) {
            throw new RuntimeException("Selected slot is already occupied");
        }
    }

    private boolean hasOverlap(
            List<Session> sessions,
            LocalDateTime targetStart,
            LocalDateTime targetEnd,
            Long excludeSessionId
    ) {
        for (Session session : sessions) {
            if (excludeSessionId != null && excludeSessionId.equals(session.getId())) {
                continue;
            }
            LocalDateTime existingStart = session.getScheduledAt();
            LocalDateTime existingEnd = existingStart.plusMinutes(resolveDuration(session.getDurationMinutes()));
            boolean overlaps = existingStart.isBefore(targetEnd) && existingEnd.isAfter(targetStart);
            if (overlaps) {
                return true;
            }
        }
        return false;
    }

    private Map<LocalDate, DayRule> loadRules(Long psychologistId, LocalDate from, LocalDate to) {
        List<PsychologistAvailability> availability = availabilityRepository
                .findByPsychologistIdAndWorkDateBetween(psychologistId, from, to);
        Map<LocalDate, DayRule> result = new HashMap<>();
        for (PsychologistAvailability item : availability) {
            boolean working = Boolean.TRUE.equals(item.getWorking());
            int startHour = item.getWorkStartHour() != null ? item.getWorkStartHour() : DEFAULT_WORK_START_HOUR;
            int endHour = item.getWorkEndHour() != null ? item.getWorkEndHour() : DEFAULT_WORK_END_HOUR;
            result.put(item.getWorkDate(), new DayRule(working, startHour, endHour));
        }
        return result;
    }

    private DayRule resolveRule(Map<LocalDate, DayRule> ruleByDate, LocalDate date) {
        DayRule explicit = ruleByDate.get(date);
        if (explicit != null) {
            return explicit;
        }
        boolean defaultWorking = isDefaultWorkingDay(date.getDayOfWeek());
        return new DayRule(defaultWorking, DEFAULT_WORK_START_HOUR, DEFAULT_WORK_END_HOUR);
    }

    private boolean isDefaultWorkingDay(DayOfWeek dayOfWeek) {
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    private Map<LocalDate, List<Session>> loadSessionsByDate(Long psychologistId, LocalDateTime from, LocalDateTime to) {
        return sessionRepository.findByPsychologistIdAndScheduledAtBetweenAndStatusIn(
                        psychologistId, from, to, ACTIVE_BOOKING_STATUSES
                )
                .stream()
                .collect(Collectors.groupingBy(session -> session.getScheduledAt().toLocalDate()));
    }

    private Map<LocalDate, List<Session>> loadScheduleSessionsByDate(Long psychologistId, LocalDateTime from, LocalDateTime to) {
        return sessionRepository.findByPsychologistIdAndScheduledAtBetweenAndStatusInWithClient(
                        psychologistId, from, to, ACTIVE_BOOKING_STATUSES
                )
                .stream()
                .collect(Collectors.groupingBy(session -> session.getScheduledAt().toLocalDate()));
    }

    private ScheduleBookingItem toBookingItem(Session session) {
        ScheduleBookingItem item = new ScheduleBookingItem();
        item.setSessionId(session.getId());
        item.setScheduledAt(session.getScheduledAt());
        item.setDurationMinutes(resolveDuration(session.getDurationMinutes()));
        item.setStatus(session.getStatus().name());
        item.setClientId(session.getClient() != null ? session.getClient().getId() : null);
        item.setClientName(session.getClient() != null ? session.getClient().getFullName() : "Сотрудник");
        item.setProblemDescription(session.getDescription());
        return item;
    }

    private int normalizeDays(Integer daysAheadRaw) {
        if (daysAheadRaw == null || daysAheadRaw <= 0) {
            return DEFAULT_DAYS_AHEAD;
        }
        return Math.min(daysAheadRaw, 31);
    }

    private int resolveDuration(Integer durationMinutes) {
        int duration = durationMinutes == null ? DEFAULT_SLOT_DURATION : durationMinutes;
        if (duration <= 0 || duration > 240) {
            throw new RuntimeException("Session duration must be between 1 and 240 minutes");
        }
        return duration;
    }

    private void validateHours(int startHour, int endHour) {
        if (startHour < 0 || startHour > 23) {
            throw new RuntimeException("Work start hour must be between 0 and 23");
        }
        if (endHour < 1 || endHour > 24) {
            throw new RuntimeException("Work end hour must be between 1 and 24");
        }
        if (startHour >= endHour) {
            throw new RuntimeException("Work end hour must be greater than start hour");
        }
    }

    private String toDayLabel(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.FULL, RU_LOCALE);
    }

    private record DayRule(boolean working, int startHour, int endHour) {
    }

    @Data
    public static class SessionRequest {
        private Long clientId;
        private LocalDateTime scheduledAt;
        private Integer durationMinutes = DEFAULT_SLOT_DURATION;
        private String description;
    }

    @Data
    public static class SessionUpdateRequest {
        private LocalDateTime scheduledAt;
        private Integer durationMinutes;
        private String description;
        private Session.SessionStatus status;
    }

    @Data
    public static class ClientBookingRequest {
        private LocalDateTime scheduledAt;
        private Integer durationMinutes = DEFAULT_SLOT_DURATION;
        private String description;
    }

    @Data
    public static class AvailableDaySlots {
        private LocalDate date;
        private String dayOfWeek;
        private Boolean working;
        private Integer workStartHour;
        private Integer workEndHour;
        private List<LocalDateTime> availableSlots;
    }

    @Data
    public static class UpdateAvailabilityRequest {
        private List<AvailabilityDayUpdate> days;
    }

    @Data
    public static class AvailabilityDayUpdate {
        private LocalDate date;
        private Boolean working;
        private Integer workStartHour;
        private Integer workEndHour;
    }

    @Data
    public static class PsychologistScheduleDay {
        private LocalDate date;
        private String dayOfWeek;
        private Boolean working;
        private Integer workStartHour;
        private Integer workEndHour;
        private List<ScheduleBookingItem> bookings;
    }

    @Data
    public static class ScheduleBookingItem {
        private Long sessionId;
        private LocalDateTime scheduledAt;
        private Integer durationMinutes;
        private String status;
        private Long clientId;
        private String clientName;
        private String problemDescription;
    }
}
