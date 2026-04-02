package com.ya.recallio.taxonomy.service;

import com.ya.recallio.common.exception.DuplicateResourceException;
import com.ya.recallio.common.exception.ResourceNotFoundException;
import com.ya.recallio.taxonomy.model.Category;
import com.ya.recallio.taxonomy.repository.CategoryRepository;
import com.ya.recallio.user.model.User;
import com.ya.recallio.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps user-owned taxonomy rules close to the category model instead of scattering them across controllers.
 */
@Service
@Transactional
public class DefaultCategoryService implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public DefaultCategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Category createCategory(CategoryCreateRequest request) {
        String name = normalizeRequiredText(request.name(), "name");

        if (categoryRepository.existsByUserIdAndNameIgnoreCase(request.userId(), name)) {
            throw new DuplicateResourceException("Category already exists with name: " + name);
        }

        Category category = new Category();
        category.setUser(resolveUser(request.userId()));
        category.setName(name);
        category.setDescription(normalizeOptionalText(request.description()));
        category.setColorHex(normalizeOptionalText(request.colorHex()));
        category.setArchived(request.archived());
        return categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Category getOwnedCategory(UUID userId, UUID categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getOwnedCategories(UUID userId, boolean includeArchived) {
        return includeArchived
                ? categoryRepository.findAllByUserIdOrderByArchivedAscNameAsc(userId)
                : categoryRepository.findAllByUserIdAndArchivedFalseOrderByNameAsc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> suggestCategories(UUID userId, String searchTerm, int limit) {
        String normalized = normalizeOptionalText(searchTerm);
        return getOwnedCategories(userId, false).stream()
                .filter(category -> normalized == null || category.getName().toLowerCase().contains(normalized.toLowerCase()))
                .limit(Math.max(1, limit))
                .toList();
    }

    private User resolveUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private String normalizeRequiredText(String value, String fieldName) {
        String normalized = normalizeOptionalText(value);

        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
