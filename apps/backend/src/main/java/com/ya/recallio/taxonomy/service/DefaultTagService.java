package com.ya.recallio.taxonomy.service;

import com.ya.recallio.common.exception.DuplicateResourceException;
import com.ya.recallio.common.exception.ResourceNotFoundException;
import com.ya.recallio.taxonomy.model.Tag;
import com.ya.recallio.taxonomy.repository.TagRepository;
import com.ya.recallio.user.model.User;
import com.ya.recallio.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps tag ownership rules and autocomplete behavior together in one domain service.
 */
@Service
@Transactional
public class DefaultTagService implements TagService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    public DefaultTagService(TagRepository tagRepository, UserRepository userRepository) {
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Tag createTag(TagCreateRequest request) {
        String name = normalizeRequiredText(request.name(), "name");

        if (tagRepository.existsByUserIdAndNameIgnoreCase(request.userId(), name)) {
            throw new DuplicateResourceException("Tag already exists with name: " + name);
        }

        Tag tag = new Tag();
        tag.setUser(resolveUser(request.userId()));
        tag.setName(name);
        tag.setColorHex(normalizeOptionalText(request.colorHex()));
        tag.setArchived(request.archived());
        return tagRepository.save(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public Tag getOwnedTag(UUID userId, UUID tagId) {
        return tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + tagId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getOwnedTags(UUID userId, boolean includeArchived) {
        return includeArchived
                ? tagRepository.findAllByUserIdOrderByArchivedAscNameAsc(userId)
                : tagRepository.findAllByUserIdAndArchivedFalseOrderByNameAsc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> suggestTags(UUID userId, String searchTerm, int limit) {
        String normalized = normalizeOptionalText(searchTerm);

        if (normalized == null) {
            return getOwnedTags(userId, false).stream().limit(Math.max(1, limit)).toList();
        }

        return tagRepository.findTop20ByUserIdAndArchivedFalseAndNameContainingIgnoreCaseOrderByNameAsc(userId, normalized)
                .stream()
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
