package com.umg.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.domain.entity.Tool;
import com.umg.domain.enums.ToolType;
import com.umg.exception.ToolExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * n8n 워크플로우 자동화 웹훅용 도구 실행 어댑터.
 *
 * <p>도구의 connectionConfig에 설정된 n8n 웹훅 URL로 HTTP POST 요청을 전송합니다.
 * 연결 설정에는 최소한 다음 항목이 포함되어야 합니다:</p>
 * <ul>
 *   <li>{@code webhookUrl} - n8n 웹훅 엔드포인트 URL</li>
 *   <li>{@code bearerToken} - 선택적 Bearer 토큰 (인증용)</li>
 * </ul>
 */
@Component
public class N8nAdapter implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(N8nAdapter.class);

    /** HTTP 요청 타임아웃 (초) */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public N8nAdapter(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * n8n 웹훅으로 HTTP POST 요청을 전송하여 도구를 실행합니다.
     *
     * @param tool        연결 설정을 포함한 도구 엔티티
     * @param params      웹훅에 전달할 입력 파라미터
     * @param userContext 사용자 컨텍스트 (n8n에서는 사용하지 않음)
     * @return 웹훅 응답 결과를 담은 CompletableFuture
     */
    @Override
    public CompletableFuture<Object> execute(Tool tool, Map<String, Object> params, String userContext) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> config = parseConnectionConfig(tool);
                String webhookUrl = (String) config.get("webhookUrl");
                if (webhookUrl == null || webhookUrl.isBlank()) {
                    throw new ToolExecutionException(tool.getName(), "connectionConfig에 webhookUrl이 없습니다");
                }

                String requestBody = objectMapper.writeValueAsString(params);

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .header("Content-Type", "application/json")
                        .timeout(REQUEST_TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody));

                /* Bearer 토큰이 설정되어 있으면 인증 헤더 추가 */
                String bearerToken = (String) config.get("bearerToken");
                if (bearerToken != null && !bearerToken.isBlank()) {
                    requestBuilder.header("Authorization", "Bearer " + bearerToken);
                }

                HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.debug("n8n 웹훅 응답 ({}): {}", response.statusCode(), response.body());
                    return (Object) response.body();
                } else {
                    throw new ToolExecutionException(tool.getName(),
                            String.format("n8n 웹훅이 HTTP %d 응답 반환: %s",
                                    response.statusCode(), response.body()));
                }
            } catch (ToolExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new ToolExecutionException(tool.getName(), "n8n 웹훅 호출 실패", e);
            }
        });
    }

    /**
     * N8N 도구 타입을 지원합니다.
     *
     * @param type 도구 타입
     * @return N8N 타입이면 true
     */
    @Override
    public boolean supports(ToolType type) {
        return type == ToolType.N8N;
    }

    /**
     * 도구의 connectionConfig JSON 문자열을 Map으로 파싱합니다.
     *
     * @param tool 도구 엔티티
     * @return 파싱된 설정 맵
     */
    private Map<String, Object> parseConnectionConfig(Tool tool) {
        try {
            return objectMapper.readValue(tool.getConnectionConfig(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new ToolExecutionException(tool.getName(), "connectionConfig JSON 파싱 실패", e);
        }
    }
}
