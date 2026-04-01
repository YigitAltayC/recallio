package com.ya.recallio.routine.model;

import com.ya.recallio.activity.model.ActivityLog;
import com.ya.recallio.common.model.BaseEntity;
import com.ya.recallio.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents one scheduled occurrence of a routine so the system can track completed and missed checks.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "routine_occurrences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_routine_occurrences_routine_scheduled_date",
                        columnNames = {"routine_id", "scheduled_date"}
                )
        },
        indexes = {
                @Index(name = "idx_routine_occurrences_user_status_start", columnList = "user_id, status, window_start_at"),
                @Index(name = "idx_routine_occurrences_routine_start", columnList = "routine_id, window_start_at")
        }
)
public class RoutineOccurrence extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @NotNull
    @Column(name = "window_start_at", nullable = false)
    private OffsetDateTime windowStartAt;

    @NotNull
    @Column(name = "window_end_at", nullable = false)
    private OffsetDateTime windowEndAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoutineOccurrenceStatus status = RoutineOccurrenceStatus.PENDING;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "missed_at")
    private OffsetDateTime missedAt;

    @Column(name = "checked_at")
    private OffsetDateTime checkedAt;

    @Size(max = 500)
    @Column(length = 500)
    private String note;

    /**
     * Links an occurrence to an optional detailed activity log without forcing every routine check to become a log entry.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_log_id", unique = true)
    private ActivityLog activityLog;
}
