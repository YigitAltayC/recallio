package com.ya.recallio.routine.service;

import com.ya.recallio.activity.model.ActivityLog;
import com.ya.recallio.activity.service.ActivityLogCreateRequest;
import com.ya.recallio.activity.service.ActivityLogService;
import com.ya.recallio.common.exception.ResourceNotFoundException;
import com.ya.recallio.routine.model.Routine;
import com.ya.recallio.routine.model.RoutineOccurrence;
import com.ya.recallio.routine.model.RoutineOccurrenceStatus;
import com.ya.recallio.routine.model.RoutineScheduleType;
import com.ya.recallio.routine.model.RoutineStatus;
import com.ya.recallio.routine.model.RoutineTimingMode;
import com.ya.recallio.routine.repository.RoutineOccurrenceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the routine checker rules: generating expected occurrences, completing them, and marking missed ones.
 */
@Service
@Transactional
public class DefaultRoutineOccurrenceService implements RoutineOccurrenceService {

    private final RoutineOccurrenceRepository routineOccurrenceRepository;
    private final RoutineService routineService;
    private final ActivityLogService activityLogService;
    private final Clock clock;

    public DefaultRoutineOccurrenceService(
            RoutineOccurrenceRepository routineOccurrenceRepository,
            RoutineService routineService,
            ActivityLogService activityLogService,
            Clock clock
    ) {
        this.routineOccurrenceRepository = routineOccurrenceRepository;
        this.routineService = routineService;
        this.activityLogService = activityLogService;
        this.clock = clock;
    }

    @Override
    public List<RoutineOccurrence> ensureOccurrences(UUID userId, OffsetDateTime windowStartAt, OffsetDateTime windowEndAt) {
        if (windowStartAt == null || windowEndAt == null || !windowEndAt.isAfter(windowStartAt)) {
            throw new IllegalArgumentException("windowEndAt must be after windowStartAt");
        }

        List<RoutineOccurrence> created = new ArrayList<>();
        List<Routine> routines = routineService.getOwnedRoutines(userId, RoutineStatus.ACTIVE);
        LocalDate startDate = windowStartAt.toLocalDate().minusDays(1);
        LocalDate endDate = windowEndAt.toLocalDate().plusDays(1);

        for (Routine routine : routines) {
            for (LocalDate candidateDate = startDate; !candidateDate.isAfter(endDate); candidateDate = candidateDate.plusDays(1)) {
                if (!shouldScheduleOn(routine, candidateDate)) {
                    continue;
                }

                if (routineOccurrenceRepository.findByRoutineIdAndScheduledDateAndUserId(
                        routine.getId(),
                        candidateDate,
                        userId
                ).isPresent()) {
                    continue;
                }

                RoutineOccurrence occurrence = buildOccurrence(routine, candidateDate);

                // Only materialize occurrences that overlap the requested window.
                if (!occurrence.getWindowEndAt().isBefore(windowStartAt) && occurrence.getWindowStartAt().isBefore(windowEndAt)) {
                    created.add(routineOccurrenceRepository.save(occurrence));
                }
            }
        }

        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoutineOccurrence> getPlannedWindow(RoutineWindowQuery query) {
        ensureOccurrences(query.userId(), query.windowStartAt(), query.windowEndAt());

        return routineOccurrenceRepository.findPlannedWindow(
                query.userId(),
                query.status(),
                query.windowStartAt(),
                query.windowEndAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoutineOccurrence> getRoutineOccurrences(UUID userId, UUID routineId) {
        routineService.getOwnedRoutine(userId, routineId);
        return routineOccurrenceRepository.findAllByRoutineIdAndUserIdOrderByWindowStartAtAsc(routineId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public RoutineOccurrence getOwnedOccurrence(UUID userId, UUID occurrenceId) {
        return routineOccurrenceRepository.findByIdAndUserId(occurrenceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Routine occurrence not found: " + occurrenceId));
    }

    @Override
    public RoutineOccurrence completeOccurrence(CompleteRoutineOccurrenceRequest request) {
        RoutineOccurrence occurrence = getOwnedOccurrence(request.userId(), request.occurrenceId());
        OffsetDateTime completedAt = request.completedAt() != null ? request.completedAt() : OffsetDateTime.now(clock);

        occurrence.setStatus(RoutineOccurrenceStatus.COMPLETED);
        occurrence.setCompletedAt(completedAt);
        occurrence.setCheckedAt(completedAt);
        occurrence.setNote(normalizeOptionalText(request.note()));

        if (request.createActivityLog() && occurrence.getActivityLog() == null) {
            ActivityLog activityLog = activityLogService.createLog(new ActivityLogCreateRequest(
                    request.userId(),
                    normalizeOptionalText(request.activityName()) != null
                            ? normalizeOptionalText(request.activityName())
                            : occurrence.getRoutine().getName(),
                    request.activityNotes(),
                    completedAt,
                    request.categoryId() != null ? request.categoryId() : categoryIdFromRoutine(occurrence.getRoutine()),
                    request.tagIds()
            ));
            occurrence.setActivityLog(activityLog);
        }

        return routineOccurrenceRepository.save(occurrence);
    }

    @Override
    public RoutineOccurrence markOccurrenceMissed(UUID userId, UUID occurrenceId, String note, OffsetDateTime missedAt) {
        RoutineOccurrence occurrence = getOwnedOccurrence(userId, occurrenceId);
        OffsetDateTime effectiveMissedAt = missedAt != null ? missedAt : OffsetDateTime.now(clock);

        occurrence.setStatus(RoutineOccurrenceStatus.MISSED);
        occurrence.setMissedAt(effectiveMissedAt);
        occurrence.setCheckedAt(effectiveMissedAt);
        occurrence.setNote(normalizeOptionalText(note));

        return routineOccurrenceRepository.save(occurrence);
    }

    @Override
    public int markOverdueOccurrences(UUID userId, OffsetDateTime referenceTime) {
        OffsetDateTime effectiveReferenceTime = referenceTime != null ? referenceTime : OffsetDateTime.now(clock);
        List<RoutineOccurrence> overduePendingOccurrences = routineOccurrenceRepository
                .findAllByUserIdAndStatusAndWindowEndAtBeforeOrderByWindowEndAtAsc(
                        userId,
                        RoutineOccurrenceStatus.PENDING,
                        effectiveReferenceTime
                );

        for (RoutineOccurrence occurrence : overduePendingOccurrences) {
            occurrence.setStatus(RoutineOccurrenceStatus.MISSED);
            occurrence.setMissedAt(effectiveReferenceTime);
            occurrence.setCheckedAt(effectiveReferenceTime);
        }

        return overduePendingOccurrences.size();
    }

    private boolean shouldScheduleOn(Routine routine, LocalDate candidateDate) {
        if (routine.getActiveUntil() != null && candidateDate.isAfter(routine.getActiveUntil().toLocalDate())) {
            return false;
        }

        if (candidateDate.isBefore(routine.getActiveFrom().toLocalDate())) {
            return false;
        }

        return switch (routine.getScheduleType()) {
            case DAILY -> isMatchingDailyCadence(routine, candidateDate);
            case WEEKLY -> isMatchingWeeklyCadence(routine, candidateDate);
            case MONTHLY -> isMatchingMonthlyCadence(routine, candidateDate);
        };
    }

    private boolean isMatchingDailyCadence(Routine routine, LocalDate candidateDate) {
        long daysBetween = ChronoUnit.DAYS.between(routine.getActiveFrom().toLocalDate(), candidateDate);
        return daysBetween % routine.getIntervalValue() == 0;
    }

    private boolean isMatchingWeeklyCadence(Routine routine, LocalDate candidateDate) {
        if (routine.getDayOfWeek() == null || candidateDate.getDayOfWeek() != routine.getDayOfWeek()) {
            return false;
        }

        LocalDate firstDate = routine.getActiveFrom().toLocalDate();
        while (firstDate.getDayOfWeek() != routine.getDayOfWeek()) {
            firstDate = firstDate.plusDays(1);
        }

        if (candidateDate.isBefore(firstDate)) {
            return false;
        }

        long weeksBetween = ChronoUnit.WEEKS.between(firstDate, candidateDate);
        return weeksBetween % routine.getIntervalValue() == 0;
    }

    private boolean isMatchingMonthlyCadence(Routine routine, LocalDate candidateDate) {
        if (routine.getDayOfMonth() == null || candidateDate.getDayOfMonth() != routine.getDayOfMonth()) {
            return false;
        }

        LocalDate firstDate = routine.getActiveFrom().toLocalDate();
        while (firstDate.getDayOfMonth() != routine.getDayOfMonth()) {
            firstDate = firstDate.plusDays(1);
            if (firstDate.getDayOfMonth() == 1 && firstDate.isAfter(candidateDate)) {
                return false;
            }
        }

        if (candidateDate.isBefore(firstDate)) {
            return false;
        }

        long monthsBetween = ChronoUnit.MONTHS.between(
                firstDate.withDayOfMonth(1),
                candidateDate.withDayOfMonth(1)
        );

        return monthsBetween % routine.getIntervalValue() == 0;
    }

    private RoutineOccurrence buildOccurrence(Routine routine, LocalDate scheduledDate) {
        OffsetDateTime windowStartAt = toOffsetDateTime(scheduledDate, startTimeFor(routine), routine.getActiveFrom().getOffset());
        OffsetDateTime windowEndAt = calculateWindowEnd(routine, scheduledDate, windowStartAt);

        RoutineOccurrence occurrence = new RoutineOccurrence();
        occurrence.setRoutine(routine);
        occurrence.setUser(routine.getUser());
        occurrence.setScheduledDate(scheduledDate);
        occurrence.setWindowStartAt(windowStartAt);
        occurrence.setWindowEndAt(windowEndAt);
        occurrence.setStatus(RoutineOccurrenceStatus.PENDING);
        return occurrence;
    }

    private LocalTime startTimeFor(Routine routine) {
        return routine.getTimingMode() == RoutineTimingMode.ANYTIME
                ? LocalTime.MIDNIGHT
                : routine.getTimeStart();
    }

    private OffsetDateTime calculateWindowEnd(Routine routine, LocalDate scheduledDate, OffsetDateTime windowStartAt) {
        return switch (routine.getTimingMode()) {
            case ANYTIME -> toOffsetDateTime(scheduledDate.plusDays(1), LocalTime.MIDNIGHT, routine.getActiveFrom().getOffset());
            case AT_TIME -> windowStartAt;
            case TIME_WINDOW -> {
                OffsetDateTime candidateEnd = toOffsetDateTime(
                        scheduledDate,
                        routine.getTimeEnd(),
                        routine.getActiveFrom().getOffset()
                );

                // Supports windows such as 22:00 -> 00:00 by rolling the end into the next day.
                if (!candidateEnd.isAfter(windowStartAt)) {
                    candidateEnd = candidateEnd.plusDays(1);
                }

                yield candidateEnd;
            }
        };
    }

    private OffsetDateTime toOffsetDateTime(LocalDate date, LocalTime time, ZoneOffset offset) {
        return OffsetDateTime.of(date, time, offset);
    }

    private UUID categoryIdFromRoutine(Routine routine) {
        return routine.getCategory() != null ? routine.getCategory().getId() : null;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
