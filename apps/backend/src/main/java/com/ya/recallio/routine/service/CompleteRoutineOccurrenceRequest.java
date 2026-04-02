package com.ya.recallio.routine.service;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Describes how an occurrence should be completed and whether a detailed activity log should be attached.
 */
public record CompleteRoutineOccurrenceRequest(
        UUID userId,
        UUID occurrenceId,
        OffsetDateTime completedAt,
        String note,
        boolean createActivityLog,
        String activityName,
        String activityNotes,
        UUID categoryId,
        Set<UUID> tagIds
) {
}
