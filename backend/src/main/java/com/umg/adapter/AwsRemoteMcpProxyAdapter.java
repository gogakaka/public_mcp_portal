package com.umg.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.domain.entity.Tool;
import com.umg.domain.enums.ToolType;
import com.umg.exception.ToolExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AWS 원격 MCP 서버 프록시 어댑터.
 *
 * <p>AWS에 호스팅된 원격 MCP 서버로 도구 호출 요청을 전달합니다.
 * AWS Signature Version 4(SigV4) 서명을 사용하여 각 요청을 인증하며,
 * 장기 자격 증명 공유 없이 안전한 교차 계정 도구 호출을 지원합니다.</p>
 *
 * <p>connectionConfig에 필요한 항목:</p>
 * <ul>
 *   <li>{@code endpointUrl} - 원격 MCP 서버 엔드포인트 URL</li>
 *   <li>{@code region} - AWS 리전 (예: us-east-1)</li>
 *   <li>{@code accessKeyId} - AWS Access Key ID</li>
 *   <li>{@code secretAccessKey} - AWS Secret Access Key</li>
 *   <li>{@code service} - AWS 서비스명 (기본값: execute-api)</li>
 * </ul>
 */
@Component
public class AwsRemoteMcpProxyAdapter implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(AwsRemoteMcpProxyAdapter.class);

    /** AWS 원격 서버 요청 타임아웃 */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /** SigV4 날짜 형식 (yyyyMMdd) */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** SigV4 타임스탬프 형식 (yyyyMMdd'T'HHmmss'Z') */
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AwsRemoteMcpProxyAdapter(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * AWS 원격 MCP 서버로 요청을 전달하여 도구를 실행합니다.
     * MCP tools/call JSON-RPC 페이로드를 구성하고 SigV4 서명을 적용합니다.
     *
     * @param tool        연결 설정을 포함한 도구 엔티티
     * @param params      MCP 서버에 전달할 입력 파라미터
     * @param userContext 사용자 컨텍스트 정보 (부서 등)
     * @return 원격 MCP 서버 응답 결과를 담은 CompletableFuture
     */
    @Override
    public CompletableFuture<Object> execute(Tool tool, Map<String, Object> params, String userContext) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> config = parseConnectionConfig(tool);
                String endpointUrl = (String) config.get("endpointUrl");
                String region = (String) config.getOrDefault("region", "us-east-1");
                String accessKeyId = (String) config.get("accessKeyId");
                String secretAccessKey = (String) config.get("secretAccessKey");
                String service = (String) config.getOrDefault("service", "execute-api");

                if (endpointUrl == null || accessKeyId == null || secretAccessKey == null) {
                    throw new ToolExecutionException(tool.getName(),
                            "필수 AWS 설정 누락: endpointUrl, accessKeyId, secretAccessKey");
                }

                /* MCP tools/call JSON-RPC 페이로드 구성 */
                Map<String, Object> mcpPayload = Map.of(
                        "jsonrpc", "2.0",
                        "id", "1",
                        "method", "tools/call",
                        "params", Map.of(
                                "name", tool.getName(),
                                "arguments", params
                        )
                );

                String requestBody = objectMapper.writeValueAsString(mcpPayload);
                URI uri = URI.create(endpointUrl);

                ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
                String dateStamp = now.format(DATE_FORMAT);
                String amzDate = now.format(DATETIME_FORMAT);

                /* 요청 본문의 SHA-256 해시 계산 */
                String payloadHash = sha256Hex(requestBody);

                /* 정규 요청 문자열 구성 */
                String host = uri.getHost();
                String path = uri.getPath().isEmpty() ? "/" : uri.getPath();

                String canonicalRequest = "POST\n"
                        + path + "\n"
                        + "\n"
                        + "content-type:application/json\n"
                        + "host:" + host + "\n"
                        + "x-amz-date:" + amzDate + "\n"
                        + "\n"
                        + "content-type;host;x-amz-date\n"
                        + payloadHash;

                /* 서명 대상 문자열 구성 */
                String credentialScope = dateStamp + "/" + region + "/" + service + "/aws4_request";
                String stringToSign = "AWS4-HMAC-SHA256\n"
                        + amzDate + "\n"
                        + credentialScope + "\n"
                        + sha256Hex(canonicalRequest);

                /* SigV4 서명 계산 */
                byte[] signingKey = getSignatureKey(secretAccessKey, dateStamp, region, service);
                String signature = HexFormat.of().formatHex(hmacSha256(signingKey, stringToSign));

                /* Authorization 헤더 구성 */
                String authHeader = "AWS4-HMAC-SHA256 Credential=" + accessKeyId + "/" + credentialScope
                        + ", SignedHeaders=content-type;host;x-amz-date"
                        + ", Signature=" + signature;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .header("Content-Type", "application/json")
                        .header("Host", host)
                        .header("X-Amz-Date", amzDate)
                        .header("X-Amz-Content-Sha256", payloadHash)
                        .header("Authorization", authHeader)
                        .timeout(REQUEST_TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.debug("AWS 원격 MCP 응답 ({}): {}", response.statusCode(),
                            response.body().substring(0, Math.min(200, response.body().length())));
                    return (Object) response.body();
                } else {
                    throw new ToolExecutionException(tool.getName(),
                            String.format("AWS 원격 MCP 서버가 HTTP %d 응답 반환: %s",
                                    response.statusCode(), response.body()));
                }
            } catch (ToolExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new ToolExecutionException(tool.getName(), "AWS 원격 MCP 서버 호출 실패", e);
            }
        });
    }

    /**
     * AWS_REMOTE 도구 타입을 지원합니다.
     *
     * @param type 도구 타입
     * @return AWS_REMOTE 타입이면 true
     */
    @Override
    public boolean supports(ToolType type) {
        return type == ToolType.AWS_REMOTE;
    }

    /**
     * SHA-256 해시를 계산하여 16진수 문자열로 반환합니다.
     *
     * @param data 해시할 데이터
     * @return 소문자 16진수 SHA-256 해시 문자열
     */
    private String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    /**
     * HMAC-SHA256을 계산합니다.
     *
     * @param key  HMAC 키 바이트 배열
     * @param data 서명할 데이터 문자열
     * @return HMAC 결과 바이트 배열
     */
    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * AWS SigV4 서명 키를 유도합니다.
     *
     * @param secretKey AWS Secret Access Key
     * @param dateStamp 날짜 (yyyyMMdd 형식)
     * @param region    AWS 리전
     * @param service   AWS 서비스명
     * @return 유도된 서명 키 바이트 배열
     */
    private byte[] getSignatureKey(String secretKey, String dateStamp, String region, String service)
            throws Exception {
        byte[] kDate = hmacSha256(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, service);
        return hmacSha256(kService, "aws4_request");
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
