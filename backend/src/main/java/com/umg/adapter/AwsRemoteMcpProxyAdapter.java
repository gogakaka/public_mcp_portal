package com.umg.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.domain.entity.AwsMcpServer;
import com.umg.domain.entity.Tool;
import com.umg.domain.enums.ToolType;
import com.umg.exception.ToolExecutionException;
import com.umg.repository.AwsMcpServerRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
 * AWS Signature Version 4(SigV4) 서명을 사용하여 각 요청을 인증합니다.</p>
 *
 * <p>도구에 {@code awsServerId}가 설정된 경우, 내부 서버 레지스트리에서
 * 연결 정보와 자격 증명을 자동으로 가져옵니다.
 * 그렇지 않으면 기존 connectionConfig를 사용합니다.</p>
 */
@Component
public class AwsRemoteMcpProxyAdapter implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(AwsRemoteMcpProxyAdapter.class);

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AwsMcpServerRepository awsMcpServerRepository;
    private final CircuitBreaker circuitBreaker;

    public AwsRemoteMcpProxyAdapter(ObjectMapper objectMapper, AwsMcpServerRepository awsMcpServerRepository,
                                     CircuitBreakerRegistry circuitBreakerRegistry) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
        this.awsMcpServerRepository = awsMcpServerRepository;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("awsMcp", "awsMcp");
    }

    /**
     * AWS 원격 MCP 서버로 요청을 전달하여 도구를 실행합니다.
     *
     * <p>도구에 내부 서버(awsServerId)가 연결된 경우 레지스트리에서
     * 엔드포인트/리전/자격 증명을 가져옵니다.</p>
     */
    @Override
    public CompletableFuture<Object> execute(Tool tool, Map<String, Object> params, String userContext) {
        return CompletableFuture.supplyAsync(() ->
                circuitBreaker.executeSupplier(() -> {
                    try {
                        AwsConnectionInfo connInfo = resolveConnectionInfo(tool);

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
                        String responseBody = signAndSend(connInfo, requestBody);

                        log.debug("AWS 원격 MCP 응답: {}",
                                responseBody.substring(0, Math.min(200, responseBody.length())));
                        return (Object) responseBody;

                    } catch (ToolExecutionException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new ToolExecutionException(tool.getName(), "AWS 원격 MCP 서버 호출 실패", e);
                    }
                })
        );
    }

    @Override
    public boolean supports(ToolType type) {
        return type == ToolType.AWS_REMOTE;
    }

    /**
     * 도구의 연결 정보를 해석합니다.
     *
     * <p>awsServerId가 설정된 경우 내부 레지스트리에서 서버를 조회하여
     * 연결 정보를 구성합니다. 그렇지 않으면 기존 connectionConfig를 사용합니다.</p>
     */
    private AwsConnectionInfo resolveConnectionInfo(Tool tool) {
        /* 내부 서버 레지스트리가 연결된 경우 */
        if (tool.getAwsServerId() != null) {
            AwsMcpServer server = awsMcpServerRepository.findById(tool.getAwsServerId())
                    .orElseThrow(() -> new ToolExecutionException(tool.getName(),
                            "연결된 AWS MCP 서버를 찾을 수 없습니다: " + tool.getAwsServerId()));

            try {
                Map<String, Object> creds = objectMapper.readValue(
                        server.getCredentialsConfig(), new TypeReference<>() {});

                log.debug("내부 AWS MCP 서버 '{}' 사용: {} ({})",
                        server.getName(), server.getEndpointUrl(), server.getRegion());

                return new AwsConnectionInfo(
                        server.getEndpointUrl(),
                        server.getRegion(),
                        server.getService(),
                        (String) creds.get("accessKeyId"),
                        (String) creds.get("secretAccessKey")
                );
            } catch (Exception e) {
                throw new ToolExecutionException(tool.getName(),
                        "내부 AWS MCP 서버 자격 증명 파싱 실패", e);
            }
        }

        /* 기존 방식: connectionConfig 직접 사용 */
        try {
            Map<String, Object> config = objectMapper.readValue(
                    tool.getConnectionConfig(), new TypeReference<>() {});
            return new AwsConnectionInfo(
                    (String) config.get("endpointUrl"),
                    (String) config.getOrDefault("region", "us-east-1"),
                    (String) config.getOrDefault("service", "execute-api"),
                    (String) config.get("accessKeyId"),
                    (String) config.get("secretAccessKey")
            );
        } catch (Exception e) {
            throw new ToolExecutionException(tool.getName(), "connectionConfig JSON 파싱 실패", e);
        }
    }

    /**
     * SigV4 서명을 적용하여 HTTP POST 요청을 전송합니다.
     */
    private String signAndSend(AwsConnectionInfo conn, String requestBody) throws Exception {
        if (conn.endpointUrl == null || conn.accessKeyId == null || conn.secretAccessKey == null) {
            throw new IllegalArgumentException("필수 AWS 설정 누락: endpointUrl, accessKeyId, secretAccessKey");
        }

        URI uri = URI.create(conn.endpointUrl);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String dateStamp = now.format(DATE_FORMAT);
        String amzDate = now.format(DATETIME_FORMAT);

        String payloadHash = sha256Hex(requestBody);
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

        String credentialScope = dateStamp + "/" + conn.region + "/" + conn.service + "/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest);

        byte[] signingKey = getSignatureKey(conn.secretAccessKey, dateStamp, conn.region, conn.service);
        String signature = HexFormat.of().formatHex(hmacSha256(signingKey, stringToSign));

        String authHeader = "AWS4-HMAC-SHA256 Credential=" + conn.accessKeyId + "/" + credentialScope
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
            return response.body();
        } else {
            throw new RuntimeException(String.format(
                    "AWS 원격 MCP 서버가 HTTP %d 응답 반환: %s",
                    response.statusCode(), response.body()));
        }
    }

    private String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] getSignatureKey(String secretKey, String dateStamp, String region, String service)
            throws Exception {
        byte[] kDate = hmacSha256(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, service);
        return hmacSha256(kService, "aws4_request");
    }

    /**
     * AWS 연결 정보를 담는 내부 레코드.
     */
    private record AwsConnectionInfo(
            String endpointUrl,
            String region,
            String service,
            String accessKeyId,
            String secretAccessKey
    ) {}
}
