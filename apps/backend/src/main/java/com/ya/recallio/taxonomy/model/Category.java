package com.ya.recallio.taxonomy.model;

import com.ya.recallio.common.model.BaseEntity;
import com.ya.recallio.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Groups related routines and activity logs under a user-owned taxonomy label.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_categories_user_name", columnNames = {"user_id", "name"})
        },
        indexes = {
                @Index(name = "idx_categories_user_archived", columnList = "user_id, archived")
        }
)
public class Category extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    @Size(max = 7)
    @Column(length = 7)
    private String colorHex;

    @Column(nullable = false)
    private boolean archived = false;
}
