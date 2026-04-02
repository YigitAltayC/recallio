package com.ya.recallio.lastdone.service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents the last known completion evidence that services can reuse without exposing persistence internals.
 */
public record LastDoneSnapshot(
        UUID activityLogId,
        UUID routineId,
        String activityName,
        OffsetDateTime occurredAt
) {
}
