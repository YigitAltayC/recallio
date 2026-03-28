package com.ya.recallio.user.repository;

import com.ya.recallio.user.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Handles persistence access for application users.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Supports login and account lookup with case-insensitive email matching.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Supports uniqueness checks before creating or updating accounts.
     */
    boolean existsByEmailIgnoreCase(String email);
}
