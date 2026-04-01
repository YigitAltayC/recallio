package com.ya.recallio.routine.repository;

import com.ya.recallio.routine.model.Routine;
import com.ya.recallio.routine.model.RoutineStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Handles persistence access for user-owned routines.
 * Query methods here only support lookup, filtering, and search concerns.
 */
public interface RoutineRepository extends JpaRepository<Routine, UUID> {

    /**
     * Resolves a routine only when it belongs to the requested user.
     */
    Optional<Routine> findByIdAndUserId(UUID routineId, UUID userId);

    /**
     * Helps services enforce user-scoped uniqueness and human-friendly lookup by name.
     */
    Optional<Routine> findByUserIdAndNameIgnoreCase(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    /**
     * Supports routine lists such as active, paused, or archived views.
     */
    List<Routine> findAllByUserIdAndStatusOrderByNameAsc(UUID userId, RoutineStatus status);

    /**
     * Supports routine search across the routine itself, schedule clues, planning context, and taxonomy references.
     * Due/overdue interpretation intentionally remains outside the repository.
     */
    @Query(
            value = """
                    select distinct routine
                    from Routine routine
                    left join routine.category category
                    left join routine.tags tag
                    where routine.user.id = :userId
                      and (:status is null or routine.status = :status)
                      and (
                          :searchTerm is null
                          or trim(:searchTerm) = ''
                          or lower(routine.name) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(routine.description, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(str(routine.dayOfWeek), '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(str(routine.dayOfMonth), '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(str(routine.scheduleType), '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(routine.placeLabel, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(routine.contextNote, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(category.name, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(tag.name, '')) like lower(concat('%', :searchTerm, '%'))
                      )
                    """,
            countQuery = """
                    select count(distinct routine.id)
                    from Routine routine
                    left join routine.category category
                    left join routine.tags tag
                    where routine.user.id = :userId
                      and (:status is null or routine.status = :status)
                      and (
                          :searchTerm is null
                          or trim(:searchTerm) = ''
                          or lower(routine.name) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(routine.description, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(str(routine.dayOfWeek), '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(str(routine.dayOfMonth), '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(str(routine.scheduleType), '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(routine.placeLabel, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(routine.contextNote, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(category.name, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(tag.name, '')) like lower(concat('%', :searchTerm, '%'))
                      )
                    """
    )
    Page<Routine> searchOwnedRoutines(
            @Param("userId") UUID userId,
            @Param("status") RoutineStatus status,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );
}
