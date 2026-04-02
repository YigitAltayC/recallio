package com.ya.recallio.routine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ya.recallio.activity.model.ActivityLog;
import com.ya.recallio.activity.service.ActivityLogService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultRoutineOccurrenceServiceTest {

    @Mock
    private RoutineOccurrenceRepository routineOccurrenceRepository;

    @Mock
    private RoutineService routineService;

    @Mock
    private ActivityLogService activityLogService;

    private DefaultRoutineOccurrenceService routineOccurrenceService;

    @BeforeEach
    void setUp() {
        routineOccurrenceService = new DefaultRoutineOccurrenceService(
                routineOccurrenceRepository,
                routineService,
                activityLogService,
                Clock.fixed(OffsetDateTime.of(2026, 4, 2, 9, 0, 0, 0, ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
        );
    }

    @Test
    void ensureOccurrencesCreatesExpectedDailyOccurrenceInsideWindow() {
        UUID userId = UUID.randomUUID();
        UUID routineId = UUID.randomUUID();

        Routine routine = new Routine();
        routine.setId(routineId);
        routine.setUser(new com.ya.recallio.user.model.User());
        routine.setName("Take out trash");
        routine.setStatus(RoutineStatus.ACTIVE);
        routine.setScheduleType(RoutineScheduleType.DAILY);
        routine.setIntervalValue(1);
        routine.setTimingMode(RoutineTimingMode.AT_TIME);
        routine.setTimeStart(LocalTime.of(23, 0));
        routine.setActiveFrom(OffsetDateTime.of(2026, 4, 2, 0, 0, 0, 0, ZoneOffset.UTC));

        when(routineService.getOwnedRoutines(userId, RoutineStatus.ACTIVE)).thenReturn(List.of(routine));
        when(routineOccurrenceRepository.findByRoutineIdAndScheduledDateAndUserId(any(), any(), any())).thenReturn(Optional.empty());
        when(routineOccurrenceRepository.save(any(RoutineOccurrence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<RoutineOccurrence> created = routineOccurrenceService.ensureOccurrences(
                userId,
                OffsetDateTime.of(2026, 4, 2, 0, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 4, 3, 0, 0, 0, 0, ZoneOffset.UTC)
        );

        assertThat(created).hasSize(1);
        assertThat(created.getFirst().getScheduledDate()).isEqualTo(LocalDate.of(2026, 4, 2));
        assertThat(created.getFirst().getStatus()).isEqualTo(RoutineOccurrenceStatus.PENDING);
    }

    @Test
    void completeOccurrenceMarksCompletedAndOptionallyCreatesActivityLog() {
        UUID userId = UUID.randomUUID();
        UUID occurrenceId = UUID.randomUUID();

        Routine routine = new Routine();
        routine.setName("Feed dog");

        RoutineOccurrence occurrence = new RoutineOccurrence();
        occurrence.setId(occurrenceId);
        occurrence.setRoutine(routine);
        occurrence.setStatus(RoutineOccurrenceStatus.PENDING);

        ActivityLog activityLog = new ActivityLog();
        activityLog.setActivityName("Feed dog");
        activityLog.setOccurredAt(OffsetDateTime.of(2026, 4, 2, 8, 30, 0, 0, ZoneOffset.UTC));

        when(routineOccurrenceRepository.findByIdAndUserId(occurrenceId, userId)).thenReturn(Optional.of(occurrence));
        when(activityLogService.createLog(any())).thenReturn(activityLog);
        when(routineOccurrenceRepository.save(any(RoutineOccurrence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoutineOccurrence completed = routineOccurrenceService.completeOccurrence(new CompleteRoutineOccurrenceRequest(
                userId,
                occurrenceId,
                OffsetDateTime.of(2026, 4, 2, 8, 30, 0, 0, ZoneOffset.UTC),
                "Done",
                true,
                null,
                "Used the new bowl",
                null,
                null
        ));

        ArgumentCaptor<RoutineOccurrence> captor = ArgumentCaptor.forClass(RoutineOccurrence.class);
        verify(routineOccurrenceRepository).save(captor.capture());

        assertThat(completed.getStatus()).isEqualTo(RoutineOccurrenceStatus.COMPLETED);
        assertThat(captor.getValue().getActivityLog()).isSameAs(activityLog);
        assertThat(captor.getValue().getCompletedAt()).isEqualTo(activityLog.getOccurredAt());
    }
}
