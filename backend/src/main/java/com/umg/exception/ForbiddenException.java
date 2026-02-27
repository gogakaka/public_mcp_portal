package com.umg.exception;

/**
 * Thrown when an authenticated user lacks the required permissions for an action.
 * Results in an HTTP 403 response.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
