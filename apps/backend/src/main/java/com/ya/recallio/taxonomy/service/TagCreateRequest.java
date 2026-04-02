package com.ya.recallio.taxonomy.service;

import java.util.UUID;

/**
 * Describes tag creation input inside the service layer.
 */
public record TagCreateRequest(
        UUID userId,
        String name,
        String colorHex,
        boolean archived
) {
}
