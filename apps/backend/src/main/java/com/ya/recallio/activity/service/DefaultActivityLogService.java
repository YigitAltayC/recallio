package com.ya.recallio.activity.service;

import com.ya.recallio.activity.model.ActivityLog;
import com.ya.recallio.activity.repository.ActivityLogRepository;
import com.ya.recallio.common.exception.ResourceNotFoundException;
import com.ya.recallio.taxonomy.model.Category;
import com.ya.recallio.taxonomy.model.Tag;
import com.ya.recallio.taxonomy.repository.CategoryRepository;
import com.ya.recallio.taxonomy.repository.TagRepository;
import com.ya.recallio.user.model.User;
import com.ya.recallio.user.repository.UserRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps activity-log business rules in one place while leaving persistence concerns in repositories.
 */
@Service
@Transactional
public class DefaultActivityLogService implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final Clock clock;

    public DefaultActivityLogService(
            ActivityLogRepository activityLogRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            TagRepository tagRepository,
            Clock clock
    ) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.clock = clock;
    }

    @Override
    public ActivityLog createLog(ActivityLogCreateRequest request) {
        User user = resolveUser(request.userId());
        String activityName = normalizeRequiredText(request.activityName(), "activityName");

        ActivityLog activityLog = new ActivityLog();
        activityLog.setUser(user);
        activityLog.setActivityName(activityName);
        activityLog.setNotes(normalizeOptionalText(request.notes()));
        activityLog.setOccurredAt(request.occurredAt() != null ? request.occurredAt() : OffsetDateTime.now(clock));
        activityLog.setCategory(resolveCategory(request.userId(), request.categoryId()));

        for (Tag tag : resolveTags(request.userId(), request.tagIds())) {
            activityLog.addTag(tag);
        }

        return activityLogRepository.save(activityLog);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityLog getOwnedLog(UUID userId, UUID activityLogId) {
        return activityLogRepository.findByIdAndUserId(activityLogId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity log not found: " + activityLogId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityLog> getOwnedHistory(ActivityHistoryQuery query) {
        Pageable pageable;
        if (query.pageable() != null)
            pageable = query.pageable();
        else
            pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "occurredAt"));

        return activityLogRepository.searchOwnedHistory(
                query.userId(),
                query.routineId(),
                query.categoryId(),
                query.occurredFrom(),
                query.occurredTo(),
                normalizeOptionalText(query.searchTerm()),
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentLogs(UUID userId, int limit) {
        int safeLimit = Math.max(1, limit);
        return activityLogRepository.searchOwnedHistory(
                        userId,
                        null,
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "occurredAt"))
                )
                .getContent();
    }

    @Override
    public void deleteOwnedLog(UUID userId, UUID activityLogId) {
        activityLogRepository.delete(getOwnedLog(userId, activityLogId));
    }

    private User resolveUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Category resolveCategory(UUID userId, UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    private Set<Tag> resolveTags(UUID userId, Set<UUID> tagIds) {
        Set<Tag> tags = new LinkedHashSet<>();

        if (tagIds == null) {
            return tags;
        }

        for (UUID tagId : tagIds) {
            Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId));
            tags.add(tag);
        }

        return tags;
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
