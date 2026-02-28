package com.umg.service;

import com.umg.adapter.ResponseShaper;
import com.umg.adapter.ToolExecutor;
import com.umg.adapter.ToolExecutorFactory;
import com.umg.domain.entity.Tool;
import com.umg.domain.entity.User;
import com.umg.domain.enums.AccessLevel;
import com.umg.domain.enums.AuditStatus;
import com.umg.domain.enums.ToolStatus;
import com.umg.dto.McpRequest;
import com.umg.dto.McpResponse;
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
 * MCP 프로토콜 핵심 처리 서비스.
 *
 * <p>Model Context Protocol에 준거하는 JSON-RPC 메시지를 처리하며,
 * 다음 메서드를 지원합니다:</p>
 * <ul>
 *   <li>{@code initialize} - 서버 기능 정보를 반환합니다.</li>
 *   <li>{@code tools/list} - 인증된 사용자가 접근 가능한 도구 목록을 반환합니다.</li>
 *   <li>{@code tools/call} - 도구를 실행하고 결과를 반환합니다.</li>
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
     * 수신된 MCP JSON-RPC 요청을 적절한 핸들러로 디스패치합니다.
     *
     * @param request MCP 요청 메시지
     * @return MCP 응답 메시지
     */
    public McpResponse handleRequest(McpRequest request) {
        if (request.getMethod() == null) {
            return McpResponse.error(request.getId(), -32600, "잘못된 요청: method 필드는 필수입니다");
        }

        return switch (request.getMethod()) {
            case "initialize" -> handleInitialize(request);
            case "tools/list" -> handleToolsList(request);
            case "tools/call" -> handleToolsCall(request);
            default -> McpResponse.error(request.getId(), -32601,
                    "존재하지 않는 메서드: " + request.getMethod());
        };
    }

    /**
     * "initialize" 메서드를 처리하여 서버 기능 정보를 반환합니다.
     *
     * @param request MCP 요청
     * @return 프로토콜 버전, 기능, 서버 정보를 포함한 응답
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
     * "tools/list" 메서드를 처리하여 현재 사용자가 접근 가능한 도구 목록을 반환합니다.
     *
     * @param request MCP 요청
     * @return 도구 설명 목록을 포함한 응답
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
     * "tools/call" 메서드를 처리하여 도구를 실행하고 결과를 반환합니다.
     *
     * <p>권한 검사, 도구 실행, 응답 가공, 감사 로그 기록을 순차적으로 수행합니다.
     * RLS(행 수준 보안)를 위해 사용자 부서 정보를 컨텍스트로 전달합니다.</p>
     *
     * @param request MCP 요청 (params에 name과 arguments 포함)
     * @return 도구 실행 결과 또는 오류 응답
     */
    @SuppressWarnings("unchecked")
    private McpResponse handleToolsCall(McpRequest request) {
        Map<String, Object> params = request.getParams();
        if (params == null || !params.containsKey("name")) {
            return McpResponse.error(request.getId(), -32602,
                    "잘못된 파라미터: tools/call에는 'name'이 필수입니다");
        }

        String toolName = (String) params.get("name");
        Map<String, Object> arguments = params.containsKey("arguments")
                ? (Map<String, Object>) params.get("arguments")
                : Collections.emptyMap();

        UUID userId = securityUtils.requireCurrentUserId();

        /* 이름으로 도구 검색 */
        Tool tool = toolRepository.findAllAccessibleByUserId(userId).stream()
                .filter(t -> t.getName().equals(toolName))
                .findFirst()
                .orElse(null);

        if (tool == null) {
            return McpResponse.error(request.getId(), -32602,
                    "도구를 찾을 수 없거나 접근 권한이 없습니다: " + toolName);
        }

        if (tool.getStatus() != ToolStatus.APPROVED) {
            return McpResponse.error(request.getId(), -32602,
                    "승인되지 않은 도구입니다: " + toolName);
        }

        /* 권한 확인 */
        if (!permissionService.hasAccess(userId, tool.getId(), AccessLevel.EXECUTE)) {
            return McpResponse.error(request.getId(), -32603,
                    "도구에 대한 접근이 거부되었습니다: " + toolName);
        }

        /* RLS를 위한 사용자 컨텍스트 조회 (부서 정보) */
        String userContext = userRepository.findById(userId)
                .map(User::getDepartment)
                .orElse(null);

        /* 도구 실행 */
        long startTime = System.currentTimeMillis();
        try {
            ToolExecutor executor = toolExecutorFactory.getExecutor(tool.getToolType());
            CompletableFuture<Object> future = executor.execute(tool, arguments, userContext);
            Object rawResult = future.join();

            /* 응답 가공 규칙 적용 */
            Object shapedResult = rawResult;
            if (tool.getResponseMappingRule() != null && !tool.getResponseMappingRule().isBlank()) {
                shapedResult = responseShaper.shape(rawResult, tool.getResponseMappingRule());
            }

            long executionTimeMs = System.currentTimeMillis() - startTime;
            auditLogService.writeAuditLog(userId, tool.getId(), tool.getName(),
                    arguments, AuditStatus.SUCCESS, null, executionTimeMs);

            log.info("도구 '{}' 실행 성공 ({}ms)", toolName, executionTimeMs);

            /* MCP 콘텐츠 응답 구성 */
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

            log.error("도구 '{}' 실행 실패 ({}ms): {}", toolName, executionTimeMs, errorMsg);

            List<Map<String, Object>> content = new ArrayList<>();
            Map<String, Object> textContent = new LinkedHashMap<>();
            textContent.put("type", "text");
            textContent.put("text", "오류: " + errorMsg);
            content.add(textContent);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            result.put("isError", true);

            return McpResponse.success(request.getId(), result);
        }
    }

    /**
     * Tool 엔티티를 MCP 도구 설명 맵으로 변환합니다.
     *
     * @param tool 도구 엔티티
     * @return MCP 도구 설명 맵 (name, description, inputSchema 포함)
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
