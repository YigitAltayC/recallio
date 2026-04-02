package com.ya.recallio.common.exception;

/**
 * Signals that a requested user-owned resource could not be resolved.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
