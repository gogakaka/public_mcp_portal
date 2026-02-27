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
 * Manages Server-Sent Event (SSE) connections for streaming MCP responses.
 *
 * <p>MCP clients may prefer SSE transport over request-response HTTP for
 * long-running tool executions. This component maintains a registry of
 * active SSE connections and provides methods to send events to them.</p>
 */
@Component
public class McpSseEmitter {

    private static final Logger log = LoggerFactory.getLogger(McpSseEmitter.class);
    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5 minutes

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public McpSseEmitter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new SSE emitter and registers it for receiving events.
     *
     * @return a new SseEmitter instance
     */
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE connection completed. Active connections: {}", emitters.size());
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.debug("SSE connection timed out. Active connections: {}", emitters.size());
        });

        emitter.onError(ex -> {
            emitters.remove(emitter);
            log.debug("SSE connection error: {}. Active connections: {}", ex.getMessage(), emitters.size());
        });

        emitters.add(emitter);
        log.debug("New SSE connection established. Active connections: {}", emitters.size());

        // Send an initial endpoint event to inform the client of the message endpoint
        try {
            emitter.send(SseEmitter.event()
                    .name("endpoint")
                    .data("/api/mcp"));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE endpoint event: {}", e.getMessage());
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * Sends an MCP response to all connected SSE clients.
     *
     * @param response the MCP response to broadcast
     */
    public void broadcast(McpResponse response) {
        String json;
        try {
            json = objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Failed to serialize MCP response for SSE: {}", e.getMessage());
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(json));
            } catch (IOException e) {
                log.debug("Failed to send SSE event, removing emitter: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
    }

    /**
     * Sends an MCP response to a specific emitter.
     *
     * @param emitter  the target SSE emitter
     * @param response the MCP response to send
     */
    public void sendToEmitter(SseEmitter emitter, McpResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(json));
        } catch (IOException e) {
            log.debug("Failed to send SSE event to specific emitter: {}", e.getMessage());
            emitters.remove(emitter);
        }
    }

    /**
     * Returns the number of active SSE connections.
     *
     * @return the count of active emitters
     */
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
