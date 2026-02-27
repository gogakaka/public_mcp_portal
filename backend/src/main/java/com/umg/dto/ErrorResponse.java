package com.umg.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response body returned by the global exception handler.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** HTTP status code. */
    private int status;

    /** Short error type (e.g. "Not Found", "Validation Error"). */
    private String error;

    /** Human-readable error message. */
    private String message;

    /** Request path that triggered the error. */
    private String path;

    /** Timestamp of the error. */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /** Trace ID for correlation with logs. */
    private String traceId;

    /** Detailed field-level validation errors, if applicable. */
    private List<FieldError> fieldErrors;

    /**
     * Represents a single field validation error.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
