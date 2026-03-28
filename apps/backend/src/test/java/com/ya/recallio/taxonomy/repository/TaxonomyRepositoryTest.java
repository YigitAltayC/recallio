package com.ya.recallio.taxonomy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ya.recallio.taxonomy.model.Category;
import com.ya.recallio.taxonomy.model.Tag;
import com.ya.recallio.user.model.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class TaxonomyRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void categoryQueriesRemainOwnershipSafeAndSorted() {
        User owner = persistUser("taxonomy-owner@example.com");
        User otherUser = persistUser("taxonomy-other@example.com");

        Category archivedCategory = persistCategory(owner, "Zeta", true);
        Category activeCategory = persistCategory(owner, "Alpha", false);
        persistCategory(otherUser, "Alpha", false);

        entityManager.flush();
        entityManager.clear();

        List<Category> categories = categoryRepository.findAllByUserIdOrderByArchivedAscNameAsc(owner.getId());

        assertThat(categories)
                .extracting(Category::getId)
                .containsExactly(activeCategory.getId(), archivedCategory.getId());
        assertThat(categoryRepository.findByIdAndUserId(archivedCategory.getId(), otherUser.getId())).isEmpty();
    }

    @Test
    void tagAutocompleteQueryOnlyReturnsActiveOwnedMatches() {
        User owner = persistUser("tag-owner@example.com");
        User otherUser = persistUser("tag-other@example.com");

        Tag matchingTag = persistTag(owner, "Morning", false);
        persistTag(owner, "Morning archived", true);
        persistTag(otherUser, "Morning", false);

        entityManager.flush();
        entityManager.clear();

        List<Tag> tags = tagRepository.findTop20ByUserIdAndArchivedFalseAndNameContainingIgnoreCaseOrderByNameAsc(
                owner.getId(),
                "morn"
        );

        assertThat(tags)
                .extracting(Tag::getId)
                .containsExactly(matchingTag.getId());
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
}
