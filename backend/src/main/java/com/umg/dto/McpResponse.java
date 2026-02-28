package com.umg.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Represents an outgoing MCP JSON-RPC response message.
 *
 * <p>Conforms to the JSON-RPC 2.0 specification. Contains either a
 * {@code result} on success or an {@code error} on failure, never both.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse {

    @JsonProperty("jsonrpc")
    @Builder.Default
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private Object id;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("error")
    private McpError error;

    /**
     * Creates a successful response.
     *
     * @param id     the request ID to echo back
     * @param result the result payload
     * @return a new McpResponse
     */
    public static McpResponse success(Object id, Object result) {
        return McpResponse.builder()
                .id(id)
                .result(result)
                .build();
    }

    /**
     * Creates an error response.
     *
     * @param id      the request ID to echo back
     * @param code    the JSON-RPC error code
     * @param message the error message
     * @return a new McpResponse
     */
    public static McpResponse error(Object id, int code, String message) {
        return McpResponse.builder()
                .id(id)
                .error(new McpError(code, message, null))
                .build();
    }

    /**
     * JSON-RPC error object.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpError {
        @JsonProperty("code")
        private int code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("data")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Object data;
    }
}
