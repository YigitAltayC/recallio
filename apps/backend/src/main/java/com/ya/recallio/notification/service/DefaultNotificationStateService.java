package com.ya.recallio.notification.service;

import com.ya.recallio.notification.model.NotificationPreference;
import com.ya.recallio.notification.repository.NotificationPreferenceRepository;
import com.ya.recallio.routine.model.RoutineOccurrence;
import com.ya.recallio.routine.model.RoutineOccurrenceStatus;
import com.ya.recallio.routine.service.RoutineOccurrenceService;
import com.ya.recallio.routine.service.RoutineWindowQuery;
import java.time.Clock;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Produces notification-ready state while keeping the actual delivery mechanism outside the backend domain logic.
 */
@Service
@Transactional
public class DefaultNotificationStateService implements NotificationStateService {

    private final RoutineOccurrenceService routineOccurrenceService;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final Clock clock;

    public DefaultNotificationStateService(
            RoutineOccurrenceService routineOccurrenceService,
            NotificationPreferenceRepository notificationPreferenceRepository,
            Clock clock
    ) {
        this.routineOccurrenceService = routineOccurrenceService;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.clock = clock;
    }

    @Override
    public List<NotificationCandidate> getCandidates(UUID userId, OffsetDateTime referenceTime) {
        OffsetDateTime effectiveReferenceTime = referenceTime != null ? referenceTime : OffsetDateTime.now(clock);
        NotificationPreference preference = notificationPreferenceRepository.findByUserId(userId).orElse(null);

        if (preference == null || isQuietHours(preference, effectiveReferenceTime.toLocalTime())) {
            return List.of();
        }

        routineOccurrenceService.ensureOccurrences(userId, effectiveReferenceTime.minusDays(1), effectiveReferenceTime.plusDays(1));
        routineOccurrenceService.markOverdueOccurrences(userId, effectiveReferenceTime);

        List<NotificationCandidate> candidates = new ArrayList<>();

        if (preference.isDueSoonAlertsEnabled()) {
            for (RoutineOccurrence occurrence : routineOccurrenceService.getPlannedWindow(new RoutineWindowQuery(
                    userId,
                    RoutineOccurrenceStatus.PENDING,
                    effectiveReferenceTime,
                    effectiveReferenceTime.plusHours(6)
            ))) {
                candidates.add(toCandidate(occurrence, NotificationState.DUE_SOON, "Routine is due soon"));
            }
        }

        if (preference.isOverdueAlertsEnabled()) {
            for (RoutineOccurrence occurrence : routineOccurrenceService.getPlannedWindow(new RoutineWindowQuery(
                    userId,
                    RoutineOccurrenceStatus.MISSED,
                    effectiveReferenceTime.minusDays(14),
                    effectiveReferenceTime
            ))) {
                candidates.add(toCandidate(occurrence, NotificationState.OVERDUE, "Routine appears overdue"));
            }
        }

        return candidates;
    }

    private NotificationCandidate toCandidate(RoutineOccurrence occurrence, NotificationState state, String message) {
        return new NotificationCandidate(
                occurrence.getId(),
                occurrence.getRoutine().getId(),
                occurrence.getRoutine().getName(),
                state,
                occurrence.getWindowStartAt(),
                occurrence.getWindowEndAt(),
                message
        );
    }

    private boolean isQuietHours(NotificationPreference preference, LocalTime currentTime) {
        if (preference.getQuietHoursStart() == null || preference.getQuietHoursEnd() == null) {
            return false;
        }

        if (preference.getQuietHoursStart().equals(preference.getQuietHoursEnd())) {
            return false;
        }

        if (preference.getQuietHoursStart().isBefore(preference.getQuietHoursEnd())) {
            return !currentTime.isBefore(preference.getQuietHoursStart()) && currentTime.isBefore(preference.getQuietHoursEnd());
        }

        return !currentTime.isBefore(preference.getQuietHoursStart()) || currentTime.isBefore(preference.getQuietHoursEnd());
    }
}
