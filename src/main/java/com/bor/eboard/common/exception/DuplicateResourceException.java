package com.bor.eboard.common.exception;

/**
 * Thrown when a unique constraint would be violated (e.g. duplicate username).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
