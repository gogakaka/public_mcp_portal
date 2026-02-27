package com.umg.exception;

/**
 * Thrown when a tool execution fails due to a backend error,
 * timeout, or unexpected response.
 * Results in an HTTP 502 response.
 */
public class ToolExecutionException extends RuntimeException {

    private final String toolName;

    public ToolExecutionException(String toolName, String message) {
        super(String.format("Tool '%s' execution failed: %s", toolName, message));
        this.toolName = toolName;
    }

    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super(String.format("Tool '%s' execution failed: %s", toolName, message), cause);
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }
}
