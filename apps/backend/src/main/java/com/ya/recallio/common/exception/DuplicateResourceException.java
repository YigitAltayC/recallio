package com.ya.recallio.common.exception;

/**
 * Signals that a user-scoped resource already exists when uniqueness should be preserved.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
