package com.ya.recallio.search.service;

import com.ya.recallio.activity.service.ActivityHistoryQuery;
import com.ya.recallio.activity.service.ActivityLogService;
import com.ya.recallio.routine.service.RoutineService;
import com.ya.recallio.taxonomy.service.CategoryService;
import com.ya.recallio.taxonomy.service.TagService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates cross-cutting search use cases while leaving each domain's query details inside its own service.
 */
@Service
@Transactional(readOnly = true)
public class DefaultSearchService implements SearchService {

    private final ActivityLogService activityLogService;
    private final RoutineService routineService;
    private final CategoryService categoryService;
    private final TagService tagService;

    public DefaultSearchService(
            ActivityLogService activityLogService,
            RoutineService routineService,
            CategoryService categoryService,
            TagService tagService
    ) {
        this.activityLogService = activityLogService;
        this.routineService = routineService;
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    @Override
    public SearchResults search(SearchQuery query) {
        return new SearchResults(
                activityLogService.getOwnedHistory(new ActivityHistoryQuery(
                        query.userId(),
                        null,
                        null,
                        null,
                        null,
                        query.searchTerm(),
                        query.activityPageable() != null
                                ? query.activityPageable()
                                : PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "occurredAt"))
                )),
                routineService.searchOwnedRoutines(
                        query.userId(),
                        query.routineStatus(),
                        query.searchTerm(),
                        query.routinePageable() != null
                                ? query.routinePageable()
                                : PageRequest.of(0, 10, Sort.by("name"))
                ),
                categoryService.suggestCategories(query.userId(), query.searchTerm(), positiveLimit(query.categorySuggestionLimit(), 10)),
                tagService.suggestTags(query.userId(), query.searchTerm(), positiveLimit(query.tagSuggestionLimit(), 10))
        );
    }

    private int positiveLimit(int value, int fallback) {
        return value > 0 ? value : fallback;
    }
}
