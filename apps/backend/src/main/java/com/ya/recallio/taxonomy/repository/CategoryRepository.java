package com.ya.recallio.taxonomy.repository;

import com.ya.recallio.taxonomy.model.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Handles persistence access for user-owned categories.
 */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Prevents cross-user category access by scoping reads to the owner.
     */
    Optional<Category> findByIdAndUserId(UUID categoryId, UUID userId);

    Optional<Category> findByUserIdAndNameIgnoreCase(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    /**
     * Keeps category lists stable and readable by returning active items first.
     */
    List<Category> findAllByUserIdOrderByArchivedAscNameAsc(UUID userId);

    List<Category> findAllByUserIdAndArchivedFalseOrderByNameAsc(UUID userId);

    /**
     * Supports pageable category screens when the UI needs archived filtering.
     */
    Page<Category> findByUserIdAndArchived(UUID userId, boolean archived, Pageable pageable);
}
