package com.umg.exception;

/**
 * Thrown when authentication fails or credentials are invalid.
 * Results in an HTTP 401 response.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
