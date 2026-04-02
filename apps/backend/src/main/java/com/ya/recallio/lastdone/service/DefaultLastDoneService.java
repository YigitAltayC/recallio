package com.ya.recallio.lastdone.service;

import com.ya.recallio.activity.model.ActivityLog;
import com.ya.recallio.activity.repository.ActivityLogRepository;
import com.ya.recallio.routine.model.RoutineOccurrence;
import com.ya.recallio.routine.model.RoutineOccurrenceStatus;
import com.ya.recallio.routine.repository.RoutineOccurrenceRepository;
import com.ya.recallio.routine.repository.RoutineRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralizes last-done lookups so dashboard, routine, and search features share one interpretation.
 */
@Service
@Transactional(readOnly = true)
public class DefaultLastDoneService implements LastDoneService {

    private final ActivityLogRepository activityLogRepository;
    private final RoutineRepository routineRepository;
    private final RoutineOccurrenceRepository routineOccurrenceRepository;

    public DefaultLastDoneService(
            ActivityLogRepository activityLogRepository,
            RoutineRepository routineRepository,
            RoutineOccurrenceRepository routineOccurrenceRepository
    ) {
        this.activityLogRepository = activityLogRepository;
        this.routineRepository = routineRepository;
        this.routineOccurrenceRepository = routineOccurrenceRepository;
    }

    @Override
    public Optional<LastDoneSnapshot> getLastDoneForRoutine(UUID userId, UUID routineId) {
        if (routineRepository.findByIdAndUserId(routineId, userId).isEmpty()) {
            return Optional.empty();
        }

        return routineOccurrenceRepository
                .findTopByRoutineIdAndUserIdAndStatusAndActivityLogIsNotNullOrderByCompletedAtDescCreatedAtDesc(
                        routineId,
                        userId,
                        RoutineOccurrenceStatus.COMPLETED
                )
                .map(this::toSnapshot);
    }

    @Override
    public Optional<LastDoneSnapshot> getLastDoneForActivityName(UUID userId, String activityName) {
        return activityLogRepository.findTopByUserIdAndActivityNameIgnoreCaseOrderByOccurredAtDescCreatedAtDesc(userId, activityName)
                .map(activityLog -> new LastDoneSnapshot(
                        activityLog.getId(),
                        null,
                        activityLog.getActivityName(),
                        activityLog.getOccurredAt()
                ));
    }

    @Override
    public Map<UUID, LastDoneSnapshot> getLastDoneForRoutines(UUID userId, Set<UUID> routineIds) {
        Map<UUID, LastDoneSnapshot> snapshots = new LinkedHashMap<>();

        if (routineIds == null) {
            return snapshots;
        }

        for (UUID routineId : routineIds) {
            getLastDoneForRoutine(userId, routineId).ifPresent(snapshot -> snapshots.put(routineId, snapshot));
        }

        return snapshots;
    }

    private LastDoneSnapshot toSnapshot(RoutineOccurrence occurrence) {
        ActivityLog activityLog = occurrence.getActivityLog();

        return new LastDoneSnapshot(
                activityLog.getId(),
                occurrence.getRoutine().getId(),
                activityLog.getActivityName(),
                activityLog.getOccurredAt()
        );
    }
}
