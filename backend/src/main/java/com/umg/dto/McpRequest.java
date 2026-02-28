package com.umg.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

/**
 * Represents an incoming MCP JSON-RPC request message.
 *
 * <p>Conforms to the JSON-RPC 2.0 specification used by the Model Context
 * Protocol. Supports methods such as {@code initialize}, {@code tools/list},
 * and {@code tools/call}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class McpRequest {

    @JsonProperty("jsonrpc")
    @Builder.Default
    private String jsonrpc = "2.0";

    @JsonProperty("id")
    private Object id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("params")
    private Map<String, Object> params;
}
