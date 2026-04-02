package com.ya.recallio.routine.service;

import com.ya.recallio.routine.model.RoutineOccurrence;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface RoutineOccurrenceService {

    List<RoutineOccurrence> ensureOccurrences(UUID userId, OffsetDateTime windowStartAt, OffsetDateTime windowEndAt);

    List<RoutineOccurrence> getPlannedWindow(RoutineWindowQuery query);

    List<RoutineOccurrence> getRoutineOccurrences(UUID userId, UUID routineId);

    RoutineOccurrence getOwnedOccurrence(UUID userId, UUID occurrenceId);

    RoutineOccurrence completeOccurrence(CompleteRoutineOccurrenceRequest request);

    RoutineOccurrence markOccurrenceMissed(UUID userId, UUID occurrenceId, String note, OffsetDateTime missedAt);

    int markOverdueOccurrences(UUID userId, OffsetDateTime referenceTime);
}
