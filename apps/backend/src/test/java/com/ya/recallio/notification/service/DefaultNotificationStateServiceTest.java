package com.ya.recallio.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ya.recallio.notification.model.NotificationPreference;
import com.ya.recallio.notification.repository.NotificationPreferenceRepository;
import com.ya.recallio.routine.model.Routine;
import com.ya.recallio.routine.model.RoutineOccurrence;
import com.ya.recallio.routine.model.RoutineOccurrenceStatus;
import com.ya.recallio.routine.service.RoutineOccurrenceService;
import java.time.Clock;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultNotificationStateServiceTest {

    @Mock
    private RoutineOccurrenceService routineOccurrenceService;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    private DefaultNotificationStateService notificationStateService;

    @BeforeEach
    void setUp() {
        notificationStateService = new DefaultNotificationStateService(
                routineOccurrenceService,
                notificationPreferenceRepository,
                Clock.fixed(OffsetDateTime.of(2026, 4, 2, 9, 0, 0, 0, ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
        );
    }

    @Test
    void getCandidatesReturnsEmptyDuringQuietHours() {
        UUID userId = UUID.randomUUID();
        NotificationPreference preference = new NotificationPreference();
        preference.setQuietHoursStart(LocalTime.of(22, 0));
        preference.setQuietHoursEnd(LocalTime.of(10, 0));

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        assertThat(notificationStateService.getCandidates(userId, null)).isEmpty();
    }

    @Test
    void getCandidatesIncludesDueSoonAndOverdueOccurrencesWhenEnabled() {
        UUID userId = UUID.randomUUID();

        NotificationPreference preference = new NotificationPreference();
        preference.setDueSoonAlertsEnabled(true);
        preference.setOverdueAlertsEnabled(true);

        Routine routine = new Routine();
        routine.setId(UUID.randomUUID());
        routine.setName("Take out trash");

        RoutineOccurrence pending = new RoutineOccurrence();
        pending.setId(UUID.randomUUID());
        pending.setRoutine(routine);
        pending.setStatus(RoutineOccurrenceStatus.PENDING);
        pending.setWindowStartAt(OffsetDateTime.of(2026, 4, 2, 12, 0, 0, 0, ZoneOffset.UTC));
        pending.setWindowEndAt(OffsetDateTime.of(2026, 4, 2, 23, 0, 0, 0, ZoneOffset.UTC));

        RoutineOccurrence missed = new RoutineOccurrence();
        missed.setId(UUID.randomUUID());
        missed.setRoutine(routine);
        missed.setStatus(RoutineOccurrenceStatus.MISSED);
        missed.setWindowStartAt(OffsetDateTime.of(2026, 4, 1, 12, 0, 0, 0, ZoneOffset.UTC));
        missed.setWindowEndAt(OffsetDateTime.of(2026, 4, 1, 23, 0, 0, 0, ZoneOffset.UTC));

        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));
        when(routineOccurrenceService.getPlannedWindow(any()))
                .thenReturn(List.of(pending))
                .thenReturn(List.of(missed));

        List<NotificationCandidate> candidates = notificationStateService.getCandidates(
                userId,
                OffsetDateTime.of(2026, 4, 2, 9, 0, 0, 0, ZoneOffset.UTC)
        );

        verify(routineOccurrenceService).markOverdueOccurrences(any(), any());
        assertThat(candidates).hasSize(2);
        assertThat(candidates).extracting(NotificationCandidate::state)
                .containsExactly(NotificationState.DUE_SOON, NotificationState.OVERDUE);
    }
}
