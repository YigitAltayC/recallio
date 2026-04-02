package com.ya.recallio.routine.service;

import com.ya.recallio.routine.model.Routine;
import com.ya.recallio.routine.model.RoutineStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoutineService {

    Routine createRoutine(RoutineCreateRequest request);

    Routine getOwnedRoutine(UUID userId, UUID routineId);

    List<Routine> getOwnedRoutines(UUID userId, RoutineStatus status);

    Page<Routine> searchOwnedRoutines(UUID userId, RoutineStatus status, String searchTerm, Pageable pageable);

    Routine updateRoutineStatus(UUID userId, UUID routineId, RoutineStatus status);
}
