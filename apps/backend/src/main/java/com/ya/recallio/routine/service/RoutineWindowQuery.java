package com.ya.recallio.routine.service;

import com.ya.recallio.routine.model.RoutineOccurrenceStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Describes a user-owned occurrence window lookup.
 */
public record RoutineWindowQuery(
        UUID userId,
        RoutineOccurrenceStatus status,
        OffsetDateTime windowStartAt,
        OffsetDateTime windowEndAt
) {
}
