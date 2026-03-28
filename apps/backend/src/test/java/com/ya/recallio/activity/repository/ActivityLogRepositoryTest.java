package com.ya.recallio.activity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ya.recallio.activity.model.ActivityLog;
import com.ya.recallio.routine.model.RecurrenceUnit;
import com.ya.recallio.routine.model.Routine;
import com.ya.recallio.routine.model.RoutineStatus;
import com.ya.recallio.taxonomy.model.Category;
import com.ya.recallio.taxonomy.model.Tag;
import com.ya.recallio.user.model.User;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class ActivityLogRepositoryTest {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findTopByUserIdAndRoutineIdOrderByOccurredAtDescCreatedAtDescReturnsLatestOwnedLog() {
        User owner = persistUser("owner@example.com");
        User otherUser = persistUser("other@example.com");

        Routine ownerRoutine = persistRoutine(owner, "Water plants");
        Routine otherRoutine = persistRoutine(otherUser, "Water plants");

        ActivityLog olderLog = persistActivityLog(owner, ownerRoutine, null, "Water plants", "Earlier log", atUtc(2026, 3, 25, 8, 0));
        ActivityLog newerLog = persistActivityLog(owner, ownerRoutine, null, "Water plants", "Latest log", atUtc(2026, 3, 28, 9, 30));
        persistActivityLog(otherUser, otherRoutine, null, "Water plants", "Foreign log", atUtc(2026, 3, 29, 7, 0));

        entityManager.flush();
        entityManager.clear();

        ActivityLog latestLog = activityLogRepository
                .findTopByUserIdAndRoutineIdOrderByOccurredAtDescCreatedAtDesc(owner.getId(), ownerRoutine.getId())
                .orElseThrow();

        assertThat(latestLog.getId()).isEqualTo(newerLog.getId());
        assertThat(latestLog.getId()).isNotEqualTo(olderLog.getId());
    }

    @Test
    void searchOwnedHistoryFiltersByOwnershipCategoryDateAndSearchTerm() {
        User owner = persistUser("history-owner@example.com");
        User otherUser = persistUser("history-other@example.com");

        Category health = persistCategory(owner, "Health", false);
        Category home = persistCategory(owner, "Home", false);
        Tag supplement = persistTag(owner, "Supplement", false);
        Tag chores = persistTag(owner, "Chores", false);
        Routine vitamins = persistRoutine(owner, "Morning vitamins");

        ActivityLog matchingLog = persistActivityLog(
                owner,
                vitamins,
                health,
                "Vitamin intake",
                "Took B12 supplement after breakfast",
                atUtc(2026, 3, 27, 8, 15),
                supplement
        );
        persistActivityLog(
                owner,
                null,
                home,
                "Kitchen cleanup",
                "Weekly sink reset",
                atUtc(2026, 3, 27, 19, 0),
                chores
        );
        persistActivityLog(
                otherUser,
                null,
                persistCategory(otherUser, "Health", false),
                "Vitamin intake",
                "Foreign data must stay hidden",
                atUtc(2026, 3, 27, 8, 30),
                persistTag(otherUser, "Supplement", false)
        );

        entityManager.flush();
        entityManager.clear();

        List<ActivityLog> results = activityLogRepository.searchOwnedHistory(
                        owner.getId(),
                        null,
                        health.getId(),
                        atUtc(2026, 3, 27, 0, 0),
                        atUtc(2026, 3, 28, 0, 0),
                        "supplement",
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "occurredAt"))
                )
                .getContent();

        assertThat(results)
                .extracting(ActivityLog::getId)
                .containsExactly(matchingLog.getId());
    }

    @Test
    void findLatestLogsForRoutineIdsReturnsOneLatestLogPerRoutineForOwner() {
        User owner = persistUser("batch-owner@example.com");
        User otherUser = persistUser("batch-other@example.com");

        Routine vitamins = persistRoutine(owner, "Vitamins");
        Routine walk = persistRoutine(owner, "Evening walk");
        Routine foreignRoutine = persistRoutine(otherUser, "Vitamins");

        ActivityLog latestVitamins = persistActivityLog(owner, vitamins, null, "Vitamins", null, atUtc(2026, 3, 29, 8, 0));
        persistActivityLog(owner, vitamins, null, "Vitamins", null, atUtc(2026, 3, 28, 8, 0));
        ActivityLog latestWalk = persistActivityLog(owner, walk, null, "Evening walk", null, atUtc(2026, 3, 28, 20, 0));
        persistActivityLog(otherUser, foreignRoutine, null, "Vitamins", null, atUtc(2026, 3, 29, 9, 0));

        entityManager.flush();
        entityManager.clear();

        List<ActivityLog> latestLogs = activityLogRepository.findLatestLogsForRoutineIds(
                owner.getId(),
                List.of(vitamins.getId(), walk.getId(), foreignRoutine.getId())
        );

        assertThat(latestLogs)
                .extracting(ActivityLog::getId)
                .containsExactlyInAnyOrder(latestVitamins.getId(), latestWalk.getId());
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setDisplayName(email);
        user.setPasswordHash("hashed-password");
        user.setTimeZone("Europe/Istanbul");
        return entityManager.persistAndFlush(user);
    }

    private Category persistCategory(User user, String name, boolean archived) {
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setArchived(archived);
        return entityManager.persistAndFlush(category);
    }

    private Tag persistTag(User user, String name, boolean archived) {
        Tag tag = new Tag();
        tag.setUser(user);
        tag.setName(name);
        tag.setArchived(archived);
        return entityManager.persistAndFlush(tag);
    }

    private Routine persistRoutine(User user, String name) {
        Routine routine = new Routine();
        routine.setUser(user);
        routine.setName(name);
        routine.setStatus(RoutineStatus.ACTIVE);
        routine.setRecurrenceInterval(1);
        routine.setRecurrenceUnit(RecurrenceUnit.DAY);
        routine.setStartAt(atUtc(2026, 3, 1, 8, 0));
        return entityManager.persistAndFlush(routine);
    }

    private ActivityLog persistActivityLog(
            User user,
            Routine routine,
            Category category,
            String activityName,
            String notes,
            OffsetDateTime occurredAt,
            Tag... tags
    ) {
        ActivityLog activityLog = new ActivityLog();
        activityLog.setUser(user);
        activityLog.setRoutine(routine);
        activityLog.setCategory(category);
        activityLog.setActivityName(activityName);
        activityLog.setNotes(notes);
        activityLog.setOccurredAt(occurredAt);

        for (Tag tag : tags) {
            activityLog.addTag(tag);
        }

        return entityManager.persistAndFlush(activityLog);
    }

    private OffsetDateTime atUtc(int year, int month, int dayOfMonth, int hour, int minute) {
        return OffsetDateTime.of(year, month, dayOfMonth, hour, minute, 0, 0, ZoneOffset.UTC);
    }
}
