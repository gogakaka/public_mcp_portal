package com.umg.controller;

import com.umg.dto.McpRequest;
import com.umg.dto.McpResponse;
import com.umg.mcp.McpSseEmitter;
import com.umg.service.McpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * MCP 프로토콜 REST 컨트롤러.
 *
 * <p>JSON-RPC 2.0 기반의 MCP 프로토콜 엔드포인트를 제공합니다.
 * 동기식 도구 호출을 위한 POST 엔드포인트와 스트리밍 응답을 위한
 * SSE(Server-Sent Events) 엔드포인트를 포함합니다.</p>
 *
 * <p>지원하는 JSON-RPC 메서드:</p>
 * <ul>
 *   <li>{@code initialize} - 서버 기능 정보 반환</li>
 *   <li>{@code tools/list} - 사용 가능한 도구 목록 반환</li>
 *   <li>{@code tools/call} - 도구 실행 및 결과 반환</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private final McpService mcpService;
    private final McpSseEmitter mcpSseEmitter;

    public McpController(McpService mcpService, McpSseEmitter mcpSseEmitter) {
        this.mcpService = mcpService;
        this.mcpSseEmitter = mcpSseEmitter;
    }

    /**
     * MCP JSON-RPC 요청을 처리합니다.
     * initialize, tools/list, tools/call 메서드를 라우팅하여 적절한 핸들러로 전달합니다.
     *
     * @param request JSON-RPC 요청 메시지
     * @return JSON-RPC 응답 메시지
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<McpResponse> handleMcpRequest(@RequestBody McpRequest request) {
        log.debug("MCP 요청 수신: method={}, id={}", request.getMethod(), request.getId());
        McpResponse response = mcpService.handleRequest(request);
        return ResponseEntity.ok(response);
    }

    /**
     * SSE 스트리밍 연결을 설정합니다.
     *
     * <p>클라이언트는 이 엔드포인트에 연결한 후, POST 엔드포인트로 JSON-RPC
     * 메시지를 전송합니다. 응답은 이 SSE 연결을 통해 스트리밍됩니다.</p>
     *
     * @return SSE 이벤트 스트림 이미터
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSse() {
        log.debug("새로운 SSE 연결 수립");
        return mcpSseEmitter.createEmitter();
    }
}
