package com.umg.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.domain.entity.CubeDataSource;
import com.umg.domain.entity.Tool;
import com.umg.domain.enums.ToolType;
import com.umg.exception.ToolExecutionException;
import com.umg.repository.CubeDataSourceRepository;
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
 * <p>도구에 {@code cubeDatasourceId}가 설정된 경우, 내부 데이터소스 레지스트리에서
 * 연결 정보를 자동으로 가져옵니다. 그렇지 않으면 기존 connectionConfig를 사용합니다.</p>
 */
@Component
public class CubeJsAdapter implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(CubeJsAdapter.class);

    /** Cube.js 쿼리는 대량 데이터를 처리할 수 있으므로 타임아웃을 넉넉하게 설정 */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final CubeDataSourceRepository cubeDataSourceRepository;

    public CubeJsAdapter(ObjectMapper objectMapper, CubeDataSourceRepository cubeDataSourceRepository) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
        this.cubeDataSourceRepository = cubeDataSourceRepository;
    }

    /**
     * Cube.js API로 분석 쿼리를 전송하여 도구를 실행합니다.
     *
     * <p>도구에 내부 데이터소스(cubeDatasourceId)가 연결된 경우 레지스트리에서
     * 연결 정보를 가져오고, 그렇지 않으면 도구의 connectionConfig를 사용합니다.</p>
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
                Map<String, Object> config = resolveConnectionConfig(tool);
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

                /* 내부 데이터소스 사용 시 DB 연결 정보를 추가 헤더로 전달 */
                if (config.containsKey("datasourceHost")) {
                    requestBuilder.header("X-Datasource-Host", (String) config.get("datasourceHost"));
                    requestBuilder.header("X-Datasource-Port", String.valueOf(config.get("datasourcePort")));
                    requestBuilder.header("X-Datasource-Database", (String) config.get("datasourceDatabase"));
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

    @Override
    public boolean supports(ToolType type) {
        return type == ToolType.CUBE_JS;
    }

    /**
     * 도구의 연결 설정을 해석합니다.
     *
     * <p>cubeDatasourceId가 설정된 경우 내부 레지스트리에서 데이터소스를 조회하여
     * 연결 정보를 구성합니다. 그렇지 않으면 기존 connectionConfig JSON을 파싱합니다.</p>
     *
     * @param tool 도구 엔티티
     * @return 연결 설정 맵
     */
    private Map<String, Object> resolveConnectionConfig(Tool tool) {
        /* 내부 데이터소스가 연결된 경우 레지스트리에서 조회 */
        if (tool.getCubeDatasourceId() != null) {
            CubeDataSource ds = cubeDataSourceRepository.findById(tool.getCubeDatasourceId())
                    .orElseThrow(() -> new ToolExecutionException(tool.getName(),
                            "연결된 데이터소스를 찾을 수 없습니다: " + tool.getCubeDatasourceId()));

            try {
                Map<String, Object> dsConfig = objectMapper.readValue(
                        ds.getConnectionConfig(), new TypeReference<>() {});

                /* 내부 데이터소스의 DB 정보를 Cube.js 연결 설정으로 변환 */
                Map<String, Object> config = new LinkedHashMap<>();
                config.put("apiUrl", dsConfig.getOrDefault("apiUrl",
                        "http://localhost:4000/cubejs-api/v1"));
                config.put("apiToken", dsConfig.get("apiToken"));
                config.put("datasourceHost", dsConfig.get("host"));
                config.put("datasourcePort", dsConfig.get("port"));
                config.put("datasourceDatabase", dsConfig.get("database"));
                config.put("datasourceUsername", dsConfig.get("username"));

                log.debug("내부 데이터소스 '{}' 사용: {}:{}/{}",
                        ds.getName(), dsConfig.get("host"), dsConfig.get("port"), dsConfig.get("database"));
                return config;
            } catch (Exception e) {
                throw new ToolExecutionException(tool.getName(),
                        "내부 데이터소스 connectionConfig 파싱 실패", e);
            }
        }

        /* 기존 방식: 도구의 connectionConfig 직접 사용 */
        return parseConnectionConfig(tool);
    }

    /**
     * 입력 파라미터에서 Cube.js 쿼리 객체를 구성합니다.
     * measures, dimensions, filters, timeDimensions, limit, offset, order, segments를 지원합니다.
     */
    private Map<String, Object> buildCubeQuery(Map<String, Object> params) {
        Map<String, Object> query = new LinkedHashMap<>();
        String[] supportedKeys = {
                "measures", "dimensions", "filters", "timeDimensions",
                "limit", "offset", "order", "segments"
        };
        for (String key : supportedKeys) {
            if (params.containsKey(key)) {
                query.put(key, params.get(key));
            }
        }
        return query;
    }

    /**
     * 도구의 connectionConfig JSON 문자열을 Map으로 파싱합니다.
     */
    private Map<String, Object> parseConnectionConfig(Tool tool) {
        try {
            return objectMapper.readValue(tool.getConnectionConfig(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new ToolExecutionException(tool.getName(), "connectionConfig JSON 파싱 실패", e);
        }
    }
}
