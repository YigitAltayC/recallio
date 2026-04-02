package com.ya.recallio.activity.service;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Describes the minimum information needed to create a user-owned activity log.
 */
public record ActivityLogCreateRequest(
        UUID userId,
        String activityName,
        String notes,
        OffsetDateTime occurredAt,
        UUID categoryId,
        Set<UUID> tagIds
) {
}
