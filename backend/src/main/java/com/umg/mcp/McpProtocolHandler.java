package com.umg.mcp;

import com.umg.dto.McpRequest;
import com.umg.dto.McpResponse;
import com.umg.service.McpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * MCP JSON-RPC 2.0 메시지 처리기.
 *
 * <p>HTTP POST 또는 SSE 전송 계층과 {@link McpService}의 비즈니스 로직 사이에서
 * 프로토콜 레이어 역할을 합니다. JSON-RPC 엔벨로프를 검증하고
 * 적절한 메서드 핸들러로 요청을 라우팅합니다.</p>
 *
 * <p>지원하는 JSON-RPC 메서드:</p>
 * <ul>
 *   <li>{@code initialize} - 서버 기능 정보 반환</li>
 *   <li>{@code tools/list} - 사용 가능한 도구 목록 반환</li>
 *   <li>{@code tools/call} - 도구 실행 및 결과 반환</li>
 * </ul>
 *
 * <p>에러 응답 코드:</p>
 * <ul>
 *   <li>{@code -32600} - 잘못된 요청 (JSON-RPC 버전 불일치, 메서드 누락)</li>
 *   <li>{@code -32601} - 존재하지 않는 메서드</li>
 *   <li>{@code -32602} - 잘못된 파라미터</li>
 *   <li>{@code -32603} - 내부 오류</li>
 * </ul>
 */
@Component
public class McpProtocolHandler {

    private static final Logger log = LoggerFactory.getLogger(McpProtocolHandler.class);

    private final McpService mcpService;

    public McpProtocolHandler(McpService mcpService) {
        this.mcpService = mcpService;
    }

    /**
     * MCP JSON-RPC 요청을 처리하고 적절한 응답을 반환합니다.
     *
     * <p>JSON-RPC 2.0 버전과 메서드 필드의 유효성을 먼저 검증한 후,
     * McpService로 요청을 위임합니다.</p>
     *
     * @param request 수신된 JSON-RPC 요청 메시지
     * @return JSON-RPC 응답 메시지
     */
    public McpResponse handle(McpRequest request) {
        /* JSON-RPC 버전 검증 */
        if (request.getJsonrpc() == null || !"2.0".equals(request.getJsonrpc())) {
            return McpResponse.error(request.getId(), -32600,
                    "잘못된 요청: jsonrpc 버전은 '2.0'이어야 합니다");
        }

        /* 메서드 필드 존재 여부 검증 */
        if (request.getMethod() == null || request.getMethod().isBlank()) {
            return McpResponse.error(request.getId(), -32600,
                    "잘못된 요청: method 필드는 필수입니다");
        }

        log.debug("MCP 요청 처리 중: method={}, id={}", request.getMethod(), request.getId());

        try {
            return mcpService.handleRequest(request);
        } catch (Exception e) {
            log.error("MCP 요청 처리 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            return McpResponse.error(request.getId(), -32603,
                    "내부 오류: " + e.getMessage());
        }
    }
}
