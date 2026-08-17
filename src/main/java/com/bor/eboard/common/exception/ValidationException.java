package com.bor.eboard.common.exception;

/**
 * Thrown when explicit service-layer validation fails.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
