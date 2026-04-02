package com.ya.recallio.search.service;

import com.ya.recallio.routine.model.RoutineStatus;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/**
 * Groups search inputs so one application search entry point can fan out to multiple domain services.
 */
public record SearchQuery(
        UUID userId,
        String searchTerm,
        RoutineStatus routineStatus,
        Pageable activityPageable,
        Pageable routinePageable,
        int categorySuggestionLimit,
        int tagSuggestionLimit
) {
}
