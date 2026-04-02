package com.ya.recallio.taxonomy.service;

import java.util.UUID;

/**
 * Describes category creation input inside the service layer.
 */
public record CategoryCreateRequest(
        UUID userId,
        String name,
        String description,
        String colorHex,
        boolean archived
) {
}
