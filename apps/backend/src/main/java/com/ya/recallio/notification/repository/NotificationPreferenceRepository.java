package com.ya.recallio.notification.repository;

import com.ya.recallio.notification.model.NotificationPreference;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Handles persistence access for notification preference records.
 */
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    /**
     * Loads preferences through the owning user so services can stay ownership-safe.
     */
    Optional<NotificationPreference> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
