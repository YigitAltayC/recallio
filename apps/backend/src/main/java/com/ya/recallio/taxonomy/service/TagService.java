package com.ya.recallio.taxonomy.service;

import com.ya.recallio.taxonomy.model.Tag;
import java.util.List;
import java.util.UUID;

public interface TagService {

    Tag createTag(TagCreateRequest request);

    Tag getOwnedTag(UUID userId, UUID tagId);

    List<Tag> getOwnedTags(UUID userId, boolean includeArchived);

    List<Tag> suggestTags(UUID userId, String searchTerm, int limit);
}
