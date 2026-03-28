package com.ya.recallio.routine.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ya.recallio.routine.model.RecurrenceUnit;
import com.ya.recallio.routine.model.Routine;
import com.ya.recallio.routine.model.RoutineStatus;
import com.ya.recallio.taxonomy.model.Category;
import com.ya.recallio.taxonomy.model.Tag;
import com.ya.recallio.user.model.User;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class RoutineRepositoryTest {

    @Autowired
    private RoutineRepository routineRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void searchOwnedRoutinesMatchesCategoryAndTagWithoutCrossUserLeakage() {
        User owner = persistUser("routine-owner@example.com");
        User otherUser = persistUser("routine-other@example.com");

        Category health = persistCategory(owner, "Health");
        Tag morning = persistTag(owner, "Morning", false);

        Routine matchingRoutine = persistRoutine(owner, "Vitamins", "Supplement reminder", RoutineStatus.ACTIVE, health, morning);
        persistRoutine(owner, "Laundry", "Weekend chores", RoutineStatus.ACTIVE, null, persistTag(owner, "Home", false));
        persistRoutine(otherUser, "Vitamins", "Foreign routine", RoutineStatus.ACTIVE, persistCategory(otherUser, "Health"), persistTag(otherUser, "Morning", false));

        entityManager.flush();
        entityManager.clear();

        List<Routine> results = routineRepository.searchOwnedRoutines(
                        owner.getId(),
                        RoutineStatus.ACTIVE,
                        "morning",
                        PageRequest.of(0, 10, Sort.by("name"))
                )
                .getContent();

        assertThat(results)
                .extracting(Routine::getId)
                .containsExactly(matchingRoutine.getId());
    }

    @Test
    void findAllByUserIdAndStatusOrderByNameAscReturnsOnlyOwnedStatusMatches() {
        User owner = persistUser("status-owner@example.com");
        User otherUser = persistUser("status-other@example.com");

        Routine archivedRoutine = persistRoutine(owner, "Archived routine", null, RoutineStatus.ARCHIVED, null);
        Routine activeARoutine = persistRoutine(owner, "Alpha routine", null, RoutineStatus.ACTIVE, null);
        Routine activeBRoutine = persistRoutine(owner, "Beta routine", null, RoutineStatus.ACTIVE, null);
        persistRoutine(otherUser, "Foreign active routine", null, RoutineStatus.ACTIVE, null);

        entityManager.flush();
        entityManager.clear();

        List<Routine> activeRoutines = routineRepository.findAllByUserIdAndStatusOrderByNameAsc(owner.getId(), RoutineStatus.ACTIVE);

        assertThat(activeRoutines)
                .extracting(Routine::getId)
                .containsExactly(activeARoutine.getId(), activeBRoutine.getId());
        assertThat(activeRoutines)
                .extracting(Routine::getId)
                .doesNotContain(archivedRoutine.getId());
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setDisplayName(email);
        user.setPasswordHash("hashed-password");
        user.setTimeZone("Europe/Istanbul");
        return entityManager.persistAndFlush(user);
    }

    private Category persistCategory(User user, String name) {
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        return entityManager.persistAndFlush(category);
    }

    private Tag persistTag(User user, String name, boolean archived) {
        Tag tag = new Tag();
        tag.setUser(user);
        tag.setName(name);
        tag.setArchived(archived);
        return entityManager.persistAndFlush(tag);
    }

    private Routine persistRoutine(User user, String name, String description, RoutineStatus status, Category category, Tag... tags) {
        Routine routine = new Routine();
        routine.setUser(user);
        routine.setName(name);
        routine.setDescription(description);
        routine.setStatus(status);
        routine.setCategory(category);
        routine.setRecurrenceInterval(1);
        routine.setRecurrenceUnit(RecurrenceUnit.DAY);
        routine.setStartAt(OffsetDateTime.of(2026, 3, 1, 8, 0, 0, 0, ZoneOffset.UTC));

        for (Tag tag : tags) {
            routine.addTag(tag);
        }

        return entityManager.persistAndFlush(routine);
    }
}
