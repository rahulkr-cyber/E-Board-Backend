package com.bor.eboard.common.exception;

/**
 * Thrown when authentication fails or is missing.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
