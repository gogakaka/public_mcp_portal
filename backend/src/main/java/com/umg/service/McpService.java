package com.umg.service;

import com.umg.adapter.ResponseShaper;
import com.umg.adapter.ToolExecutor;
import com.umg.adapter.ToolExecutorFactory;
import com.umg.domain.entity.Tool;
import com.umg.domain.enums.AccessLevel;
import com.umg.domain.enums.AuditStatus;
import com.umg.domain.enums.ToolStatus;
import com.umg.dto.McpRequest;
import com.umg.dto.McpResponse;
import com.umg.domain.entity.User;
import com.umg.exception.ForbiddenException;
import com.umg.exception.ResourceNotFoundException;
import com.umg.exception.ToolExecutionException;
import com.umg.repository.ToolRepository;
import com.umg.repository.UserRepository;
import com.umg.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Core MCP protocol handling service.
 *
 * <p>Processes JSON-RPC messages conforming to the Model Context Protocol,
 * supporting the following methods:</p>
 * <ul>
 *   <li>{@code initialize} - Returns server capabilities.</li>
 *   <li>{@code tools/list} - Lists tools accessible to the authenticated user.</li>
 *   <li>{@code tools/call} - Executes a tool and returns the result.</li>
 * </ul>
 */
@Service
public class McpService {

    private static final Logger log = LoggerFactory.getLogger(McpService.class);

    private final ToolRepository toolRepository;
    private final UserRepository userRepository;
    private final ToolService toolService;
    private final PermissionService permissionService;
    private final AuditLogService auditLogService;
    private final ToolExecutorFactory toolExecutorFactory;
    private final ResponseShaper responseShaper;
    private final SecurityUtils securityUtils;

    public McpService(ToolRepository toolRepository,
                      UserRepository userRepository,
                      ToolService toolService,
                      PermissionService permissionService,
                      AuditLogService auditLogService,
                      ToolExecutorFactory toolExecutorFactory,
                      ResponseShaper responseShaper,
                      SecurityUtils securityUtils) {
        this.toolRepository = toolRepository;
        this.userRepository = userRepository;
        this.toolService = toolService;
        this.permissionService = permissionService;
        this.auditLogService = auditLogService;
        this.toolExecutorFactory = toolExecutorFactory;
        this.responseShaper = responseShaper;
        this.securityUtils = securityUtils;
    }

    /**
     * Handles an incoming MCP JSON-RPC request and dispatches to the appropriate handler.
     *
     * @param request the MCP request
     * @return the MCP response
     */
    public McpResponse handleRequest(McpRequest request) {
        if (request.getMethod() == null) {
            return McpResponse.error(request.getId(), -32600, "Invalid request: method is required");
        }

        return switch (request.getMethod()) {
            case "initialize" -> handleInitialize(request);
            case "tools/list" -> handleToolsList(request);
            case "tools/call" -> handleToolsCall(request);
            default -> McpResponse.error(request.getId(), -32601,
                    "Method not found: " + request.getMethod());
        };
    }

    /**
     * Handles the "initialize" method, returning server capabilities.
     */
    private McpResponse handleInitialize(McpRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("capabilities", Map.of(
                "tools", Map.of("listChanged", false)
        ));
        result.put("serverInfo", Map.of(
                "name", "Universal MCP Gateway",
                "version", "0.1.0"
        ));
        return McpResponse.success(request.getId(), result);
    }

    /**
     * Handles the "tools/list" method, returning tools accessible to the current user.
     */
    private McpResponse handleToolsList(McpRequest request) {
        UUID userId = securityUtils.requireCurrentUserId();
        List<Tool> tools = toolService.listAllAccessibleTools(userId);

        List<Map<String, Object>> toolDescriptions = tools.stream()
                .map(this::toolToMcpDescription)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tools", toolDescriptions);
        return McpResponse.success(request.getId(), result);
    }

    /**
     * Handles the "tools/call" method, executing a tool and returning the result.
     */
    @SuppressWarnings("unchecked")
    private McpResponse handleToolsCall(McpRequest request) {
        Map<String, Object> params = request.getParams();
        if (params == null || !params.containsKey("name")) {
            return McpResponse.error(request.getId(), -32602,
                    "Invalid params: 'name' is required for tools/call");
        }

        String toolName = (String) params.get("name");
        Map<String, Object> arguments = params.containsKey("arguments")
                ? (Map<String, Object>) params.get("arguments")
                : Collections.emptyMap();

        UUID userId = securityUtils.requireCurrentUserId();

        // Find the tool by name
        Tool tool = toolRepository.findAllAccessibleByUserId(userId).stream()
                .filter(t -> t.getName().equals(toolName))
                .findFirst()
                .orElse(null);

        if (tool == null) {
            return McpResponse.error(request.getId(), -32602,
                    "Tool not found or not accessible: " + toolName);
        }

        if (tool.getStatus() != ToolStatus.APPROVED) {
            return McpResponse.error(request.getId(), -32602,
                    "Tool is not approved for execution: " + toolName);
        }

        // Check permission
        if (!permissionService.hasAccess(userId, tool.getId(), AccessLevel.EXECUTE)) {
            return McpResponse.error(request.getId(), -32603,
                    "Access denied for tool: " + toolName);
        }

        // Resolve user context for RLS (department)
        String userContext = userRepository.findById(userId)
                .map(User::getDepartment)
                .orElse(null);

        // Execute the tool
        long startTime = System.currentTimeMillis();
        try {
            ToolExecutor executor = toolExecutorFactory.getExecutor(tool.getToolType());
            CompletableFuture<Object> future = executor.execute(tool, arguments, userContext);
            Object rawResult = future.join();

            // Apply response shaping if configured
            Object shapedResult = rawResult;
            if (tool.getResponseMappingRule() != null && !tool.getResponseMappingRule().isBlank()) {
                shapedResult = responseShaper.shape(rawResult, tool.getResponseMappingRule());
            }

            long executionTimeMs = System.currentTimeMillis() - startTime;
            auditLogService.writeAuditLog(userId, tool.getId(), tool.getName(),
                    arguments, AuditStatus.SUCCESS, null, executionTimeMs);

            log.info("Tool '{}' executed successfully in {}ms", toolName, executionTimeMs);

            // Build MCP content response
            List<Map<String, Object>> content = new ArrayList<>();
            Map<String, Object> textContent = new LinkedHashMap<>();
            textContent.put("type", "text");
            textContent.put("text", shapedResult instanceof String
                    ? (String) shapedResult
                    : shapedResult.toString());
            content.add(textContent);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            result.put("isError", false);

            return McpResponse.success(request.getId(), result);

        } catch (Exception e) {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();

            auditLogService.writeAuditLog(userId, tool.getId(), tool.getName(),
                    arguments, AuditStatus.FAIL, errorMsg, executionTimeMs);

            log.error("Tool '{}' execution failed in {}ms: {}", toolName, executionTimeMs, errorMsg);

            List<Map<String, Object>> content = new ArrayList<>();
            Map<String, Object> textContent = new LinkedHashMap<>();
            textContent.put("type", "text");
            textContent.put("text", "Error: " + errorMsg);
            content.add(textContent);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            result.put("isError", true);

            return McpResponse.success(request.getId(), result);
        }
    }

    /**
     * Converts a Tool entity to an MCP tool description map.
     */
    private Map<String, Object> toolToMcpDescription(Tool tool) {
        Map<String, Object> desc = new LinkedHashMap<>();
        desc.put("name", tool.getName());
        desc.put("description", tool.getDescription() != null ? tool.getDescription() : "");

        if (tool.getInputSchema() != null && !tool.getInputSchema().isEmpty()) {
            desc.put("inputSchema", tool.getInputSchema());
        } else {
            desc.put("inputSchema", Map.of(
                    "type", "object",
                    "properties", Collections.emptyMap()
            ));
        }

        return desc;
    }
}
