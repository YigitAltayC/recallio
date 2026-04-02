package com.ya.recallio.taxonomy.service;

import com.ya.recallio.taxonomy.model.Category;
import java.util.List;
import java.util.UUID;

public interface CategoryService {

    Category createCategory(CategoryCreateRequest request);

    Category getOwnedCategory(UUID userId, UUID categoryId);

    List<Category> getOwnedCategories(UUID userId, boolean includeArchived);

    List<Category> suggestCategories(UUID userId, String searchTerm, int limit);
}
