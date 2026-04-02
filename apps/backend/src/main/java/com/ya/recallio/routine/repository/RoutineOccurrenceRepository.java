package com.ya.recallio.routine.repository;

import com.ya.recallio.routine.model.RoutineOccurrence;
import com.ya.recallio.routine.model.RoutineOccurrenceStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Handles persistence for user-owned routine occurrences and lightweight checker-oriented reads.
 */
public interface RoutineOccurrenceRepository extends JpaRepository<RoutineOccurrence, UUID> {

    Optional<RoutineOccurrence> findByIdAndUserId(UUID occurrenceId, UUID userId);

    Optional<RoutineOccurrence> findByRoutineIdAndScheduledDateAndUserId(UUID routineId, LocalDate scheduledDate, UUID userId);

    Optional<RoutineOccurrence> findTopByRoutineIdAndUserIdAndStatusAndActivityLogIsNotNullOrderByCompletedAtDescCreatedAtDesc(
            UUID routineId,
            UUID userId,
            RoutineOccurrenceStatus status
    );

    List<RoutineOccurrence> findAllByRoutineIdAndUserIdOrderByWindowStartAtAsc(UUID routineId, UUID userId);

    List<RoutineOccurrence> findAllByUserIdAndStatusAndWindowEndAtBeforeOrderByWindowEndAtAsc(
            UUID userId,
            RoutineOccurrenceStatus status,
            OffsetDateTime windowEndAt
    );

    /**
     * Loads occurrences in a time window so services can decide which reminders or calendar checks to show.
     */
    @Query("""
            select occurrence
            from RoutineOccurrence occurrence
            join fetch occurrence.routine routine
            where occurrence.user.id = :userId
              and (:status is null or occurrence.status = :status)
              and occurrence.windowStartAt >= :windowStartAt
              and occurrence.windowStartAt < :windowEndAt
            order by occurrence.windowStartAt asc
            """)
    List<RoutineOccurrence> findPlannedWindow(
            @Param("userId") UUID userId,
            @Param("status") RoutineOccurrenceStatus status,
            @Param("windowStartAt") OffsetDateTime windowStartAt,
            @Param("windowEndAt") OffsetDateTime windowEndAt
    );
}
