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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Cube.js 시맨틱 레이어 분석 쿼리용 도구 실행 어댑터.
 *
 * <p>입력 파라미터를 Cube.js 쿼리 형식으로 변환하여 Cube.js REST API로 전송합니다.
 * 행 수준 보안(RLS) 적용을 위해 {@code X-User-Context} 헤더에
 * 사용자 부서 정보를 주입합니다.</p>
 *
 * <p>connectionConfig에 필요한 항목:</p>
 * <ul>
 *   <li>{@code apiUrl} - Cube.js REST API 기본 URL (예: http://localhost:4000/cubejs-api/v1)</li>
 *   <li>{@code apiToken} - Cube.js API 토큰</li>
 * </ul>
 */
@Component
public class CubeJsAdapter implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(CubeJsAdapter.class);

    /** Cube.js 쿼리는 대량 데이터를 처리할 수 있으므로 타임아웃을 넉넉하게 설정 */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CubeJsAdapter(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Cube.js API로 분석 쿼리를 전송하여 도구를 실행합니다.
     * measures, dimensions, filters 등의 파라미터를 Cube.js 쿼리 형식으로 변환합니다.
     *
     * @param tool        연결 설정을 포함한 도구 엔티티
     * @param params      Cube.js 쿼리 파라미터 (measures, dimensions, filters 등)
     * @param userContext 사용자 부서 정보 (X-User-Context 헤더로 주입, RLS 적용용)
     * @return Cube.js 쿼리 결과를 담은 CompletableFuture
     */
    @Override
    public CompletableFuture<Object> execute(Tool tool, Map<String, Object> params, String userContext) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> config = parseConnectionConfig(tool);
                String apiUrl = (String) config.get("apiUrl");
                String apiToken = (String) config.get("apiToken");

                if (apiUrl == null || apiUrl.isBlank()) {
                    throw new ToolExecutionException(tool.getName(), "connectionConfig에 apiUrl이 없습니다");
                }

                /* 입력 파라미터를 Cube.js 쿼리 형식으로 변환 */
                Map<String, Object> cubeQuery = buildCubeQuery(params);
                String queryJson = objectMapper.writeValueAsString(Map.of("query", cubeQuery));

                String loadUrl = apiUrl.endsWith("/") ? apiUrl + "load" : apiUrl + "/load";

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(loadUrl))
                        .header("Content-Type", "application/json")
                        .timeout(REQUEST_TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofString(queryJson));

                /* API 토큰이 있으면 인증 헤더 추가 */
                if (apiToken != null && !apiToken.isBlank()) {
                    requestBuilder.header("Authorization", apiToken);
                }

                /* RLS 적용을 위한 사용자 컨텍스트 헤더 주입 */
                if (userContext != null && !userContext.isBlank()) {
                    requestBuilder.header("X-User-Context", userContext);
                }

                HttpResponse<String> response = httpClient.send(requestBuilder.build(),
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.debug("Cube.js 응답 ({}): {}", response.statusCode(),
                            response.body().substring(0, Math.min(200, response.body().length())));
                    return (Object) response.body();
                } else {
                    throw new ToolExecutionException(tool.getName(),
                            String.format("Cube.js API가 HTTP %d 응답 반환: %s",
                                    response.statusCode(), response.body()));
                }
            } catch (ToolExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new ToolExecutionException(tool.getName(), "Cube.js 쿼리 실행 실패", e);
            }
        });
    }

    /**
     * CUBE_JS 도구 타입을 지원합니다.
     *
     * @param type 도구 타입
     * @return CUBE_JS 타입이면 true
     */
    @Override
    public boolean supports(ToolType type) {
        return type == ToolType.CUBE_JS;
    }

    /**
     * 입력 파라미터에서 Cube.js 쿼리 객체를 구성합니다.
     * measures, dimensions, filters, timeDimensions, limit, offset, order를 지원합니다.
     *
     * @param params 입력 파라미터 맵
     * @return Cube.js 쿼리 맵
     */
    private Map<String, Object> buildCubeQuery(Map<String, Object> params) {
        Map<String, Object> query = new LinkedHashMap<>();

        if (params.containsKey("measures")) {
            query.put("measures", params.get("measures"));
        }
        if (params.containsKey("dimensions")) {
            query.put("dimensions", params.get("dimensions"));
        }
        if (params.containsKey("filters")) {
            query.put("filters", params.get("filters"));
        }
        if (params.containsKey("timeDimensions")) {
            query.put("timeDimensions", params.get("timeDimensions"));
        }
        if (params.containsKey("limit")) {
            query.put("limit", params.get("limit"));
        }
        if (params.containsKey("offset")) {
            query.put("offset", params.get("offset"));
        }
        if (params.containsKey("order")) {
            query.put("order", params.get("order"));
        }

        return query;
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
