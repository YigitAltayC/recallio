package com.ya.recallio.routine.model;

import com.ya.recallio.common.model.BaseEntity;
import com.ya.recallio.taxonomy.model.Category;
import com.ya.recallio.taxonomy.model.Tag;
import com.ya.recallio.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
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
 * Describes a repeatable activity that can be evaluated for due and overdue states.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "routines",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_routines_user_name", columnNames = {"user_id", "name"})
        },
        indexes = {
                @Index(name = "idx_routines_user_status", columnList = "user_id, status")
        }
)
public class Routine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToMany
    @JoinTable(
            name = "routine_tags",
            joinColumns = @JoinColumn(name = "routine_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new LinkedHashSet<>();

    @NotBlank
    @Size(max = 140)
    @Column(nullable = false, length = 140)
    private String name;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoutineStatus status = RoutineStatus.ACTIVE;

    @Min(1)
    @Column(nullable = false)
    private Integer recurrenceInterval = 1;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RecurrenceUnit recurrenceUnit = RecurrenceUnit.DAY;

    @NotNull
    @Column(nullable = false)
    private OffsetDateTime startAt;

    @Min(0)
    private Integer dueSoonLeadTimeMinutes;

    @Column(nullable = false)
    private boolean warningEnabled = true;

    public void addTag(Tag tag) {
        if (tag != null) {
            tags.add(tag);
        }
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
    }
}
