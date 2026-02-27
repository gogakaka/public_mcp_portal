package com.umg.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * MCP JSON-RPC message models representing the various structured types
 * exchanged between the MCP client and server.
 */
public final class McpMessage {

    private McpMessage() {
    }

    /**
     * Server capability declaration returned during initialization.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServerCapabilities {
        @JsonProperty("tools")
        private ToolCapability tools;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ToolCapability {
            @JsonProperty("listChanged")
            private boolean listChanged;
        }
    }

    /**
     * Server information returned during initialization.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServerInfo {
        @JsonProperty("name")
        private String name;

        @JsonProperty("version")
        private String version;
    }

    /**
     * MCP tool description returned by tools/list.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolDescription {
        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("inputSchema")
        private Map<String, Object> inputSchema;
    }

    /**
     * Content block within a tools/call response.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentBlock {
        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private String text;

        @JsonProperty("mimeType")
        private String mimeType;

        @JsonProperty("data")
        private String data;
    }

    /**
     * Result of a tools/call response containing content blocks.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CallResult {
        @JsonProperty("content")
        private List<ContentBlock> content;

        @JsonProperty("isError")
        @Builder.Default
        private boolean isError = false;
    }

    /**
     * Result of a tools/list response containing tool descriptions.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListResult {
        @JsonProperty("tools")
        private List<ToolDescription> tools;
    }
}
