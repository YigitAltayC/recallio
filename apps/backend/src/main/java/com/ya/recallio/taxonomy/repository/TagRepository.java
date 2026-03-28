package com.ya.recallio.taxonomy.repository;

import com.ya.recallio.taxonomy.model.Tag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Handles persistence access for user-owned tags.
 */
public interface TagRepository extends JpaRepository<Tag, UUID> {

    /**
     * Prevents cross-user tag access by scoping reads to the owner.
     */
    Optional<Tag> findByIdAndUserId(UUID tagId, UUID userId);

    Optional<Tag> findByUserIdAndNameIgnoreCase(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    List<Tag> findAllByUserIdOrderByArchivedAscNameAsc(UUID userId);

    List<Tag> findAllByUserIdAndArchivedFalseOrderByNameAsc(UUID userId);

    /**
     * Supports lightweight autocomplete/search suggestions for active tags only.
     */
    List<Tag> findTop20ByUserIdAndArchivedFalseAndNameContainingIgnoreCaseOrderByNameAsc(UUID userId, String nameFragment);
}
