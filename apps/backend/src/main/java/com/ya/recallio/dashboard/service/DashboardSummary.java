package com.ya.recallio.dashboard.service;

import com.ya.recallio.activity.model.ActivityLog;
import com.ya.recallio.routine.model.RoutineOccurrence;
import java.util.List;

/**
 * Represents the high-level dashboard state that the UI can later map into response DTOs.
 */
public record DashboardSummary(
        List<ActivityLog> recentLogs,
        List<RoutineOccurrence> dueSoonOccurrences,
        List<RoutineOccurrence> overdueOccurrences,
        long activeRoutineCount,
        long pendingOccurrenceCount,
        long completedOccurrenceCount
) {
}
