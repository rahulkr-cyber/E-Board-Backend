package com.bor.eboard.common.exception;

/**
 * Thrown when a business rule is violated.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
