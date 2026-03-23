package com.ya.recallio.notification.model;

import com.ya.recallio.common.model.BaseEntity;
import com.ya.recallio.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores user-controlled notification preferences without mixing delivery logic into the user entity.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "notification_preferences",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_preferences_user", columnNames = "user_id")
        }
)
public class NotificationPreference extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean browserChannelEnabled = true;

    @Column(nullable = false)
    private boolean desktopChannelEnabled = true;

    @Column(nullable = false)
    private boolean dueSoonAlertsEnabled = true;

    @Column(nullable = false)
    private boolean overdueAlertsEnabled = true;

    @Column(nullable = false)
    private boolean staleActivityAlertsEnabled = true;

    private LocalTime quietHoursStart;

    private LocalTime quietHoursEnd;

    @Min(15)
    @Column(nullable = false)
    private Integer minimumIntervalMinutes = 180;

    public void setUser(User user) {
        this.user = user;

        if (user != null && user.getNotificationPreference() != this) {
            user.assignNotificationPreference(this);
        }
    }
}
