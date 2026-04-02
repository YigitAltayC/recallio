package com.ya.recallio.routine.service;

import com.ya.recallio.common.exception.DuplicateResourceException;
import com.ya.recallio.common.exception.ResourceNotFoundException;
import com.ya.recallio.routine.model.Routine;
import com.ya.recallio.routine.model.RoutineScheduleType;
import com.ya.recallio.routine.model.RoutineStatus;
import com.ya.recallio.routine.model.RoutineTimingMode;
import com.ya.recallio.routine.repository.RoutineRepository;
import com.ya.recallio.taxonomy.model.Category;
import com.ya.recallio.taxonomy.model.Tag;
import com.ya.recallio.taxonomy.repository.CategoryRepository;
import com.ya.recallio.taxonomy.repository.TagRepository;
import com.ya.recallio.user.model.User;
import com.ya.recallio.user.repository.UserRepository;
import java.time.LocalTime;
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
 * Owns routine-plan validation and keeps scheduling rules out of controllers and repositories.
 */
@Service
@Transactional
public class DefaultRoutineService implements RoutineService {

    private final RoutineRepository routineRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    public DefaultRoutineService(
            RoutineRepository routineRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            TagRepository tagRepository
    ) {
        this.routineRepository = routineRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    @Override
    public Routine createRoutine(RoutineCreateRequest request) {
        String name = normalizeRequiredText(request.name(), "name");
        validateSchedule(request);

        User user = resolveUser(request.userId());

        if (routineRepository.existsByUserIdAndNameIgnoreCase(request.userId(), name)) {
            throw new DuplicateResourceException("Routine already exists with name: " + name);
        }

        Routine routine = new Routine();
        routine.setUser(user);
        routine.setName(name);
        routine.setDescription(normalizeOptionalText(request.description()));
        routine.setStatus(request.status() != null ? request.status() : RoutineStatus.ACTIVE);
        routine.setIntervalValue(request.intervalValue() != null ? request.intervalValue() : 1);
        routine.setScheduleType(request.scheduleType());
        routine.setDayOfWeek(request.scheduleType() == RoutineScheduleType.WEEKLY ? request.dayOfWeek() : null);
        routine.setDayOfMonth(request.scheduleType() == RoutineScheduleType.MONTHLY ? request.dayOfMonth() : null);
        routine.setTimingMode(request.timingMode());
        routine.setTimeStart(request.timingMode() == RoutineTimingMode.ANYTIME ? null : request.timeStart());
        routine.setTimeEnd(request.timingMode() == RoutineTimingMode.TIME_WINDOW ? request.timeEnd() : null);
        routine.setPlaceLabel(normalizeOptionalText(request.placeLabel()));
        routine.setContextNote(normalizeOptionalText(request.contextNote()));
        routine.setActiveFrom(request.activeFrom());
        routine.setActiveUntil(request.activeUntil());
        routine.setDueSoonLeadTimeMinutes(request.dueSoonLeadTimeMinutes());
        routine.setWarningEnabled(request.warningEnabled());
        routine.setCategory(resolveCategory(request.userId(), request.categoryId()));

        for (Tag tag : resolveTags(request.userId(), request.tagIds())) {
            routine.addTag(tag);
        }

        return routineRepository.save(routine);
    }

    @Override
    @Transactional(readOnly = true)
    public Routine getOwnedRoutine(UUID userId, UUID routineId) {
        return routineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Routine not found: " + routineId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Routine> getOwnedRoutines(UUID userId, RoutineStatus status) {
        if (status == null) {
            return routineRepository.searchOwnedRoutines(
                            userId,
                            null,
                            null,
                            PageRequest.of(0, 500, Sort.by("name"))
                    )
                    .getContent();
        }

        return routineRepository.findAllByUserIdAndStatusOrderByNameAsc(userId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Routine> searchOwnedRoutines(UUID userId, RoutineStatus status, String searchTerm, Pageable pageable) {
        Pageable safePageable = pageable != null ? pageable : PageRequest.of(0, 20, Sort.by("name"));
        return routineRepository.searchOwnedRoutines(userId, status, normalizeOptionalText(searchTerm), safePageable);
    }

    @Override
    public Routine updateRoutineStatus(UUID userId, UUID routineId, RoutineStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        Routine routine = getOwnedRoutine(userId, routineId);
        routine.setStatus(status);
        return routineRepository.save(routine);
    }

    private void validateSchedule(RoutineCreateRequest request) {
        if (request.userId() == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        if (request.scheduleType() == null) {
            throw new IllegalArgumentException("scheduleType must not be null");
        }

        if (request.timingMode() == null) {
            throw new IllegalArgumentException("timingMode must not be null");
        }

        if (request.intervalValue() == null || request.intervalValue() < 1) {
            throw new IllegalArgumentException("intervalValue must be at least 1");
        }

        if (request.activeFrom() == null) {
            throw new IllegalArgumentException("activeFrom must not be null");
        }

        if (request.activeUntil() != null && request.activeUntil().isBefore(request.activeFrom())) {
            throw new IllegalArgumentException("activeUntil must not be before activeFrom");
        }

        if (request.dueSoonLeadTimeMinutes() != null && request.dueSoonLeadTimeMinutes() < 0) {
            throw new IllegalArgumentException("dueSoonLeadTimeMinutes must not be negative");
        }

        if (request.scheduleType() == RoutineScheduleType.WEEKLY && request.dayOfWeek() == null) {
            throw new IllegalArgumentException("dayOfWeek is required for weekly routines");
        }

        if (request.scheduleType() == RoutineScheduleType.MONTHLY) {
            if (request.dayOfMonth() == null || request.dayOfMonth() < 1 || request.dayOfMonth() > 31) {
                throw new IllegalArgumentException("dayOfMonth must be between 1 and 31 for monthly routines");
            }
        }

        if (request.timingMode() == RoutineTimingMode.AT_TIME) {
            requireTime(request.timeStart(), "timeStart is required for AT_TIME");
        }

        if (request.timingMode() == RoutineTimingMode.TIME_WINDOW) {
            requireTime(request.timeStart(), "timeStart is required for TIME_WINDOW");
            requireTime(request.timeEnd(), "timeEnd is required for TIME_WINDOW");

            if (request.timeStart().equals(request.timeEnd())) {
                throw new IllegalArgumentException("timeStart and timeEnd must not be equal for TIME_WINDOW");
            }
        }
    }

    private void requireTime(LocalTime value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
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
