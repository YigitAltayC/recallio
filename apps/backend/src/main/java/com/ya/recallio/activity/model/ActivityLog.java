package com.ya.recallio.activity.model;

import com.ya.recallio.common.model.BaseEntity;
import com.ya.recallio.routine.model.Routine;
import com.ya.recallio.taxonomy.model.Category;
import com.ya.recallio.taxonomy.model.Tag;
import com.ya.recallio.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Captures a historical activity event that can later be searched and used for last-done calculations.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "activity_logs",
        indexes = {
                @Index(name = "idx_activity_logs_user_name_occurred", columnList = "user_id, activity_name, occurred_at"),
                @Index(name = "idx_activity_logs_user_occurred", columnList = "user_id, occurred_at")
        }
)
public class ActivityLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private Routine routine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany
    @JoinTable(
            name = "activity_log_tags",
            joinColumns = @JoinColumn(name = "activity_log_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new LinkedHashSet<>();

    @NotBlank
    @Size(max = 140)
    @Column(name = "activity_name", nullable = false, length = 140)
    private String activityName;

    @Size(max = 2000)
    @Column(length = 2000)
    private String notes;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    public void addTag(Tag tag) {
        if (tag != null) {
            tags.add(tag);
        }
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
    }
}
