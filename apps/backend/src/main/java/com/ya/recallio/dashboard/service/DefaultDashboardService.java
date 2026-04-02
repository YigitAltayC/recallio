package com.ya.recallio.dashboard.service;

import com.ya.recallio.activity.model.ActivityLog;
import com.ya.recallio.activity.service.ActivityLogService;
import com.ya.recallio.routine.model.RoutineOccurrence;
import com.ya.recallio.routine.model.RoutineOccurrenceStatus;
import com.ya.recallio.routine.model.RoutineStatus;
import com.ya.recallio.routine.service.RoutineOccurrenceService;
import com.ya.recallio.routine.service.RoutineService;
import com.ya.recallio.routine.service.RoutineWindowQuery;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds dashboard-ready state by composing smaller domain services instead of pushing aggregation into repositories.
 */
@Service
@Transactional(readOnly = true)
public class DefaultDashboardService implements DashboardService {

    private final ActivityLogService activityLogService;
    private final RoutineService routineService;
    private final RoutineOccurrenceService routineOccurrenceService;
    private final Clock clock;

    public DefaultDashboardService(
            ActivityLogService activityLogService,
            RoutineService routineService,
            RoutineOccurrenceService routineOccurrenceService,
            Clock clock
    ) {
        this.activityLogService = activityLogService;
        this.routineService = routineService;
        this.routineOccurrenceService = routineOccurrenceService;
        this.clock = clock;
    }

    @Override
    public DashboardSummary buildSummary(UUID userId, OffsetDateTime referenceTime) {
        OffsetDateTime effectiveReferenceTime = referenceTime != null ? referenceTime : OffsetDateTime.now(clock);
        OffsetDateTime dueSoonWindowEnd = effectiveReferenceTime.plusDays(1);

        List<ActivityLog> recentLogs = activityLogService.getRecentLogs(userId, 10);
        List<RoutineOccurrence> dueSoonOccurrences = routineOccurrenceService.getPlannedWindow(new RoutineWindowQuery(
                userId,
                RoutineOccurrenceStatus.PENDING,
                effectiveReferenceTime,
                dueSoonWindowEnd
        ));
        List<RoutineOccurrence> overdueOccurrences = routineOccurrenceService.getPlannedWindow(new RoutineWindowQuery(
                userId,
                RoutineOccurrenceStatus.MISSED,
                effectiveReferenceTime.minusDays(30),
                effectiveReferenceTime
        ));
        long activeRoutineCount = routineService.getOwnedRoutines(userId, RoutineStatus.ACTIVE).size();
        long pendingOccurrenceCount = dueSoonOccurrences.size();
        long completedOccurrenceCount = routineOccurrenceService.getPlannedWindow(new RoutineWindowQuery(
                userId,
                RoutineOccurrenceStatus.COMPLETED,
                effectiveReferenceTime.minusDays(30),
                dueSoonWindowEnd
        )).size();

        return new DashboardSummary(
                recentLogs,
                dueSoonOccurrences,
                overdueOccurrences,
                activeRoutineCount,
                pendingOccurrenceCount,
                completedOccurrenceCount
        );
    }
}
