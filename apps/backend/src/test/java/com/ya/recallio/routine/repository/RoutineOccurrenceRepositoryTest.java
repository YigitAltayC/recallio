package com.ya.recallio.routine.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ya.recallio.activity.model.ActivityLog;
import com.ya.recallio.routine.model.Routine;
import com.ya.recallio.routine.model.RoutineOccurrence;
import com.ya.recallio.routine.model.RoutineOccurrenceStatus;
import com.ya.recallio.routine.model.RoutineScheduleType;
import com.ya.recallio.routine.model.RoutineStatus;
import com.ya.recallio.routine.model.RoutineTimingMode;
import com.ya.recallio.user.model.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class RoutineOccurrenceRepositoryTest {

    @Autowired
    private RoutineOccurrenceRepository routineOccurrenceRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByRoutineIdAndScheduledDateAndUserIdRemainsOwnershipSafe() {
        User owner = persistUser("occurrence-owner@example.com");
        User otherUser = persistUser("occurrence-other@example.com");

        Routine ownerRoutine = persistRoutine(owner, "Take out trash");
        Routine otherRoutine = persistRoutine(otherUser, "Take out trash");

        RoutineOccurrence ownerOccurrence = persistOccurrence(
                owner,
                ownerRoutine,
                LocalDate.of(2026, 4, 2),
                RoutineOccurrenceStatus.COMPLETED
        );
        persistOccurrence(otherUser, otherRoutine, LocalDate.of(2026, 4, 2), RoutineOccurrenceStatus.COMPLETED);

        entityManager.flush();
        entityManager.clear();

        assertThat(
                routineOccurrenceRepository.findByRoutineIdAndScheduledDateAndUserId(
                        ownerRoutine.getId(),
                        LocalDate.of(2026, 4, 2),
                        owner.getId()
                )
        )
                .get()
                .extracting(RoutineOccurrence::getId)
                .isEqualTo(ownerOccurrence.getId());

        assertThat(
                routineOccurrenceRepository.findByRoutineIdAndScheduledDateAndUserId(
                        ownerRoutine.getId(),
                        LocalDate.of(2026, 4, 2),
                        otherUser.getId()
                )
        ).isEmpty();
    }

    @Test
    void findPlannedWindowReturnsOnlyRequestedStatusInsideWindow() {
        User owner = persistUser("window-owner@example.com");

        Routine routine = persistRoutine(owner, "Feed dog");
        RoutineOccurrence pendingOccurrence = persistOccurrence(
                owner,
                routine,
                LocalDate.of(2026, 4, 3),
                RoutineOccurrenceStatus.PENDING
        );
        persistOccurrence(owner, routine, LocalDate.of(2026, 4, 4), RoutineOccurrenceStatus.MISSED);

        entityManager.flush();
        entityManager.clear();

        List<RoutineOccurrence> occurrences = routineOccurrenceRepository.findPlannedWindow(
                owner.getId(),
                RoutineOccurrenceStatus.PENDING,
                atUtc(2026, 4, 3, 0, 0),
                atUtc(2026, 4, 4, 0, 0)
        );

        assertThat(occurrences)
                .extracting(RoutineOccurrence::getId)
                .containsExactly(pendingOccurrence.getId());
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setDisplayName(email);
        user.setPasswordHash("hashed-password");
        user.setTimeZone("Europe/Istanbul");
        return entityManager.persistAndFlush(user);
    }

    private Routine persistRoutine(User user, String name) {
        Routine routine = new Routine();
        routine.setUser(user);
        routine.setName(name);
        routine.setStatus(RoutineStatus.ACTIVE);
        routine.setScheduleType(RoutineScheduleType.DAILY);
        routine.setIntervalValue(1);
        routine.setTimingMode(RoutineTimingMode.AT_TIME);
        routine.setTimeStart(LocalTime.of(23, 0));
        routine.setActiveFrom(atUtc(2026, 4, 1, 0, 0));
        return entityManager.persistAndFlush(routine);
    }

    private RoutineOccurrence persistOccurrence(
            User user,
            Routine routine,
            LocalDate scheduledDate,
            RoutineOccurrenceStatus status
    ) {
        RoutineOccurrence occurrence = new RoutineOccurrence();
        occurrence.setUser(user);
        occurrence.setRoutine(routine);
        occurrence.setScheduledDate(scheduledDate);
        occurrence.setWindowStartAt(OffsetDateTime.of(scheduledDate, LocalTime.of(23, 0), ZoneOffset.UTC));
        occurrence.setWindowEndAt(OffsetDateTime.of(scheduledDate.plusDays(1), LocalTime.MIDNIGHT, ZoneOffset.UTC));
        occurrence.setStatus(status);
        occurrence.setCheckedAt(occurrence.getWindowEndAt());

        if (status == RoutineOccurrenceStatus.COMPLETED) {
            ActivityLog activityLog = new ActivityLog();
            activityLog.setUser(user);
            activityLog.setActivityName(routine.getName());
            activityLog.setOccurredAt(occurrence.getWindowStartAt());
            entityManager.persistAndFlush(activityLog);
            occurrence.setActivityLog(activityLog);
            occurrence.setCompletedAt(activityLog.getOccurredAt());
        }

        if (status == RoutineOccurrenceStatus.MISSED) {
            occurrence.setMissedAt(occurrence.getWindowEndAt());
        }

        return entityManager.persistAndFlush(occurrence);
    }

    private OffsetDateTime atUtc(int year, int month, int dayOfMonth, int hour, int minute) {
        return OffsetDateTime.of(year, month, dayOfMonth, hour, minute, 0, 0, ZoneOffset.UTC);
    }
}
