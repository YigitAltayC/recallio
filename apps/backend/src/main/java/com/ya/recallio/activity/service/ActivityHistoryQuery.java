package com.ya.recallio.activity.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/**
 * Carries user-scoped history filters without binding the service layer to HTTP models.
 */
public record ActivityHistoryQuery(
        UUID userId,
        UUID routineId,
        UUID categoryId,
        OffsetDateTime occurredFrom,
        OffsetDateTime occurredTo,
        String searchTerm,
        Pageable pageable
) {
}
