package com.umg.mcp;

import com.umg.dto.McpRequest;
import com.umg.dto.McpResponse;
import com.umg.service.McpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handles MCP JSON-RPC messages by dispatching them to the appropriate
 * service method.
 *
 * <p>This component acts as the protocol layer between the transport
 * (HTTP POST or SSE) and the business logic in {@link McpService}.
 * It validates the JSON-RPC envelope and delegates processing.</p>
 */
@Component
public class McpProtocolHandler {

    private static final Logger log = LoggerFactory.getLogger(McpProtocolHandler.class);

    private final McpService mcpService;

    public McpProtocolHandler(McpService mcpService) {
        this.mcpService = mcpService;
    }

    /**
     * Processes an MCP JSON-RPC request and returns the appropriate response.
     *
     * @param request the incoming JSON-RPC request
     * @return the JSON-RPC response
     */
    public McpResponse handle(McpRequest request) {
        // Validate JSON-RPC version
        if (request.getJsonrpc() == null || !"2.0".equals(request.getJsonrpc())) {
            return McpResponse.error(request.getId(), -32600,
                    "Invalid request: jsonrpc version must be '2.0'");
        }

        // Validate method is present
        if (request.getMethod() == null || request.getMethod().isBlank()) {
            return McpResponse.error(request.getId(), -32600,
                    "Invalid request: method is required");
        }

        log.debug("Processing MCP request: method={}, id={}", request.getMethod(), request.getId());

        try {
            return mcpService.handleRequest(request);
        } catch (Exception e) {
            log.error("Unexpected error processing MCP request: {}", e.getMessage(), e);
            return McpResponse.error(request.getId(), -32603,
                    "Internal error: " + e.getMessage());
        }
    }
}
