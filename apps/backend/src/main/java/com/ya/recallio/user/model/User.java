package com.ya.recallio.user.model;

import com.ya.recallio.common.model.BaseEntity;
import com.ya.recallio.notification.model.NotificationPreference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an authenticated application user and holds account-level settings.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
public class User extends BaseEntity {

    @Email
    @NotBlank
    @Size(max = 320)
    @Column(nullable = false, length = 320)
    private String email;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String displayName;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String passwordHash;

    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String timeZone = "Europe/Istanbul";

    @Column(nullable = false)
    private boolean enabled = true;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private NotificationPreference notificationPreference;

    public void assignNotificationPreference(NotificationPreference notificationPreference) {
        this.notificationPreference = notificationPreference;

        if (notificationPreference != null && notificationPreference.getUser() != this) {
            notificationPreference.setUser(this);
        }
    }
}
