package com.ya.recallio.routine.service;

import com.ya.recallio.routine.model.RoutineScheduleType;
import com.ya.recallio.routine.model.RoutineStatus;
import com.ya.recallio.routine.model.RoutineTimingMode;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Captures the business input required to create or validate a routine plan.
 */
public record RoutineCreateRequest(
        UUID userId,
        String name,
        String description,
        RoutineStatus status,
        Integer intervalValue,
        RoutineScheduleType scheduleType,
        DayOfWeek dayOfWeek,
        Integer dayOfMonth,
        RoutineTimingMode timingMode,
        LocalTime timeStart,
        LocalTime timeEnd,
        String placeLabel,
        String contextNote,
        OffsetDateTime activeFrom,
        OffsetDateTime activeUntil,
        Integer dueSoonLeadTimeMinutes,
        boolean warningEnabled,
        UUID categoryId,
        Set<UUID> tagIds
) {
}
