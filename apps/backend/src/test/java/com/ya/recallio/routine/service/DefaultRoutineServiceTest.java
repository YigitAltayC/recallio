package com.ya.recallio.routine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultRoutineServiceTest {

    @Mock
    private RoutineRepository routineRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private DefaultRoutineService routineService;

    @Test
    void createRoutineRejectsWeeklyScheduleWithoutDayOfWeek() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> routineService.createRoutine(new RoutineCreateRequest(
                userId,
                "Trash",
                null,
                RoutineStatus.ACTIVE,
                1,
                RoutineScheduleType.WEEKLY,
                null,
                null,
                RoutineTimingMode.AT_TIME,
                LocalTime.of(23, 0),
                null,
                null,
                null,
                OffsetDateTime.now(ZoneOffset.UTC),
                null,
                30,
                true,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dayOfWeek");
    }

    @Test
    void createRoutineBuildsOwnedRoutineWithResolvedCategoryAndTags() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();

        User user = new User();
        Category category = new Category();
        Tag tag = new Tag();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(category));
        when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.of(tag));
        when(routineRepository.existsByUserIdAndNameIgnoreCase(userId, "Feed dog")).thenReturn(false);
        when(routineRepository.save(any(Routine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Routine routine = routineService.createRoutine(new RoutineCreateRequest(
                userId,
                "Feed dog",
                "Daily reminder",
                RoutineStatus.ACTIVE,
                1,
                RoutineScheduleType.DAILY,
                null,
                null,
                RoutineTimingMode.TIME_WINDOW,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0),
                "Kitchen",
                "Before leaving home",
                OffsetDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                null,
                60,
                true,
                categoryId,
                Set.of(tagId)
        ));

        ArgumentCaptor<Routine> captor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository).save(captor.capture());

        assertThat(routine.getName()).isEqualTo("Feed dog");
        assertThat(captor.getValue().getCategory()).isSameAs(category);
        assertThat(captor.getValue().getTags()).containsExactly(tag);
        assertThat(captor.getValue().getTimingMode()).isEqualTo(RoutineTimingMode.TIME_WINDOW);
    }
}
