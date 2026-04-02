package com.ya.recallio.search.service;

import com.ya.recallio.activity.model.ActivityLog;
import com.ya.recallio.routine.model.Routine;
import com.ya.recallio.taxonomy.model.Category;
import com.ya.recallio.taxonomy.model.Tag;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Aggregates search results from multiple bounded contexts without forcing one giant repository query.
 */
public record SearchResults(
        Page<ActivityLog> activityLogs,
        Page<Routine> routines,
        List<Category> categories,
        List<Tag> tags
) {
}
