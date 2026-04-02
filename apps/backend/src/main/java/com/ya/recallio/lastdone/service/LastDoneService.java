package com.ya.recallio.lastdone.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface LastDoneService {

    Optional<LastDoneSnapshot> getLastDoneForRoutine(UUID userId, UUID routineId);

    Optional<LastDoneSnapshot> getLastDoneForActivityName(UUID userId, String activityName);

    Map<UUID, LastDoneSnapshot> getLastDoneForRoutines(UUID userId, Set<UUID> routineIds);
}
