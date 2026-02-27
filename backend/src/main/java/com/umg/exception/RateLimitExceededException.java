package com.umg.exception;

/**
 * Thrown when a client exceeds the configured rate limit.
 * Results in an HTTP 429 response.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }

    public RateLimitExceededException() {
        super("Rate limit exceeded. Please try again later.");
    }
}
