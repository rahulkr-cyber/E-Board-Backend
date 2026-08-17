package com.bor.eboard.common.exception;

/**
 * Thrown when an authenticated user lacks the required permission.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
