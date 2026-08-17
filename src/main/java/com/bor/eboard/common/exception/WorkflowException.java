package com.bor.eboard.common.exception;

/**
 * Thrown for invalid workflow operations.
 */
public class WorkflowException extends RuntimeException {

    public WorkflowException(String message) {
        super(message);
    }
}
