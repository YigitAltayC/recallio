package com.ya.recallio.notification.service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents one warning candidate that the UI or desktop wrapper can decide to display.
 */
public record NotificationCandidate(
        UUID occurrenceId,
        UUID routineId,
        String routineName,
        NotificationState state,
        OffsetDateTime windowStartAt,
        OffsetDateTime windowEndAt,
        String message
) {
}
