package com.umg.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.dto.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE(Server-Sent Events) 연결 관리자.
 *
 * <p>MCP 클라이언트가 장시간 실행되는 도구 호출에 대해 요청-응답 방식의 HTTP 대신
 * SSE 전송 방식을 선택할 수 있도록 지원합니다. 이 컴포넌트는 활성 SSE 연결의
 * 레지스트리를 유지하며, 연결 등록, 해제, 브로드캐스트 기능을 제공합니다.</p>
 *
 * <p>SSE 연결 수명 주기:</p>
 * <ul>
 *   <li>클라이언트가 GET /api/mcp/sse로 연결을 수립하면 초기 endpoint 이벤트 전송</li>
 *   <li>완료, 타임아웃, 오류 발생 시 자동으로 연결 해제</li>
 *   <li>기본 타임아웃: 5분</li>
 * </ul>
 */
@Component
public class McpSseEmitter {

    private static final Logger log = LoggerFactory.getLogger(McpSseEmitter.class);

    /** SSE 연결 타임아웃 (5분) */
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L;

    /** 스레드 안전한 활성 이미터 목록 */
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public McpSseEmitter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 새로운 SSE 이미터를 생성하고 이벤트 수신을 위해 등록합니다.
     *
     * <p>연결 수립 후 초기 endpoint 이벤트를 전송하여 클라이언트에게
     * JSON-RPC 메시지를 전송할 엔드포인트 경로를 알립니다.</p>
     *
     * @return 새로 생성된 SseEmitter 인스턴스
     */
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        /* 연결 완료 시 콜백 */
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE 연결 완료. 활성 연결 수: {}", emitters.size());
        });

        /* 연결 타임아웃 시 콜백 */
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("SSE 연결 타임아웃. 활성 연결 수: {}", emitters.size());
        });

        /* 연결 오류 시 콜백 */
        emitter.onError(ex -> {
            emitters.remove(emitter);
            log.debug("SSE 연결 오류: {}. 활성 연결 수: {}", ex.getMessage(), emitters.size());
        });

        emitters.add(emitter);
        log.debug("새로운 SSE 연결 수립. 활성 연결 수: {}", emitters.size());

        /* 클라이언트에게 메시지 전송 엔드포인트를 알리는 초기 이벤트 전송 */
        try {
            emitter.send(SseEmitter.event()
                    .name("endpoint")
                    .data("/api/mcp"));
        } catch (IOException e) {
            log.warn("초기 SSE endpoint 이벤트 전송 실패: {}", e.getMessage());
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * 모든 연결된 SSE 클라이언트에게 MCP 응답을 브로드캐스트합니다.
     *
     * <p>전송에 실패한 이미터는 자동으로 목록에서 제거됩니다.</p>
     *
     * @param response 브로드캐스트할 MCP 응답
     */
    public void broadcast(McpResponse response) {
        String json;
        try {
            json = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("SSE 전송을 위한 MCP 응답 직렬화 실패: {}", e.getMessage());
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(json));
            } catch (IOException e) {
                log.debug("SSE 이벤트 전송 실패, 이미터 제거: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
    }

    /**
     * 특정 이미터에 MCP 응답을 전송합니다.
     *
     * @param emitter  대상 SSE 이미터
     * @param response 전송할 MCP 응답
     */
    public void sendToEmitter(SseEmitter emitter, McpResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(json));
        } catch (IOException e) {
            log.debug("특정 이미터로 SSE 이벤트 전송 실패: {}", e.getMessage());
            emitters.remove(emitter);
        }
    }

    /**
     * 현재 활성 SSE 연결 수를 반환합니다.
     *
     * @return 활성 이미터 수
     */
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
