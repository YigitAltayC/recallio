package com.ya.recallio.activity.repository;

import com.ya.recallio.activity.model.ActivityLog;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Handles persistence access for historical activity logs.
 * The repository stays focused on user-scoped reads and write support,
 * while higher-level recall and due/overdue rules remain in services.
 */
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    /**
     * Resolves a single log only when it belongs to the given user.
     */
    Optional<ActivityLog> findByIdAndUserId(UUID activityLogId, UUID userId);

    /**
     * Supports routine-based last-done lookup with a deterministic tie-breaker through the occurrence link.
     */
    @Query("""
            select activityLog
            from ActivityLog activityLog
            join RoutineOccurrence occurrence on occurrence.activityLog = activityLog
            where activityLog.user.id = :userId
              and occurrence.routine.id = :routineId
            order by activityLog.occurredAt desc, activityLog.createdAt desc
            """)
    List<ActivityLog> findLatestRoutineLinkedLogs(
            @Param("userId") UUID userId,
            @Param("routineId") UUID routineId,
            Pageable pageable
    );

    default Optional<ActivityLog> findTopByUserIdAndRoutineIdOrderByOccurredAtDescCreatedAtDesc(UUID userId, UUID routineId) {
        return findLatestRoutineLinkedLogs(userId, routineId, Pageable.ofSize(1)).stream().findFirst();
    }

    /**
     * Supports free-form activity last-done lookup for logs not necessarily tied to a routine.
     */
    Optional<ActivityLog> findTopByUserIdAndActivityNameIgnoreCaseOrderByOccurredAtDescCreatedAtDesc(UUID userId, String activityName);

    /**
     * Provides pageable history/search access without leaking other users' data.
     * Search stays intentionally broad but simple enough for the initial product scope.
     */
    @Query(
            value = """
                    select distinct activityLog
                    from ActivityLog activityLog
                    left join activityLog.category category
                    left join activityLog.tags tag
                    left join RoutineOccurrence occurrence on occurrence.activityLog = activityLog
                    left join occurrence.routine routine
                    where activityLog.user.id = :userId
                      and (:routineId is null or routine.id = :routineId)
                      and (:categoryId is null or category.id = :categoryId)
                      and (:occurredFrom is null or activityLog.occurredAt >= :occurredFrom)
                      and (:occurredTo is null or activityLog.occurredAt <= :occurredTo)
                      and (
                          :searchTerm is null
                          or trim(:searchTerm) = ''
                          or lower(activityLog.activityName) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(activityLog.notes, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(category.name, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(tag.name, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(routine.name, '')) like lower(concat('%', :searchTerm, '%'))
                      )
                    """,
            countQuery = """
                    select count(distinct activityLog.id)
                    from ActivityLog activityLog
                    left join activityLog.category category
                    left join activityLog.tags tag
                    left join RoutineOccurrence occurrence on occurrence.activityLog = activityLog
                    left join occurrence.routine routine
                    where activityLog.user.id = :userId
                      and (:routineId is null or routine.id = :routineId)
                      and (:categoryId is null or category.id = :categoryId)
                      and (:occurredFrom is null or activityLog.occurredAt >= :occurredFrom)
                      and (:occurredTo is null or activityLog.occurredAt <= :occurredTo)
                      and (
                          :searchTerm is null
                          or trim(:searchTerm) = ''
                          or lower(activityLog.activityName) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(activityLog.notes, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(category.name, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(tag.name, '')) like lower(concat('%', :searchTerm, '%'))
                          or lower(coalesce(routine.name, '')) like lower(concat('%', :searchTerm, '%'))
                      )
                    """
    )
    Page<ActivityLog> searchOwnedHistory(
            @Param("userId") UUID userId,
            @Param("routineId") UUID routineId,
            @Param("categoryId") UUID categoryId,
            @Param("occurredFrom") OffsetDateTime occurredFrom,
            @Param("occurredTo") OffsetDateTime occurredTo,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    /**
     * Loads one latest log per routine so routine evaluation can stay in the service layer without N+1 lookups.
     */
    @Query("""
            select activityLog
            from ActivityLog activityLog
            join RoutineOccurrence occurrence on occurrence.activityLog = activityLog
            where activityLog.user.id = :userId
              and occurrence.routine.id in :routineIds
              and activityLog.occurredAt = (
                  select max(candidate.occurredAt)
                  from ActivityLog candidate
                  join RoutineOccurrence candidateOccurrence on candidateOccurrence.activityLog = candidate
                  where candidate.user.id = :userId
                    and candidateOccurrence.routine.id = occurrence.routine.id
              )
              and activityLog.createdAt = (
                  select max(candidateAtSameTime.createdAt)
                  from ActivityLog candidateAtSameTime
                  join RoutineOccurrence candidateAtSameTimeOccurrence on candidateAtSameTimeOccurrence.activityLog = candidateAtSameTime
                  where candidateAtSameTime.user.id = :userId
                    and candidateAtSameTimeOccurrence.routine.id = occurrence.routine.id
                    and candidateAtSameTime.occurredAt = activityLog.occurredAt
              )
            """)
    List<ActivityLog> findLatestLogsForRoutineIds(@Param("userId") UUID userId, @Param("routineIds") Collection<UUID> routineIds);
}
