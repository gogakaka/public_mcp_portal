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
 * Tool executor adapter for remote AWS MCP servers with SigV4 request signing.
 *
 * <p>Forwards tool invocations to a remote MCP server hosted on AWS,
 * signing each request using AWS Signature Version 4. This enables
 * secure cross-account tool invocation without sharing long-lived
 * credentials.</p>
 *
 * <p>Expected connection config:</p>
 * <ul>
 *   <li>{@code endpointUrl} - The remote MCP server endpoint</li>
 *   <li>{@code region} - AWS region (e.g. us-east-1)</li>
 *   <li>{@code accessKeyId} - AWS access key ID</li>
 *   <li>{@code secretAccessKey} - AWS secret access key</li>
 *   <li>{@code service} - AWS service name (default: execute-api)</li>
 * </ul>
 */
@Component
public class AwsRemoteMcpProxyAdapter implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(AwsRemoteMcpProxyAdapter.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AwsRemoteMcpProxyAdapter(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

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
                            "Missing required AWS config: endpointUrl, accessKeyId, secretAccessKey");
                }

                // Build the MCP tools/call JSON-RPC payload
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

                // Calculate content hash
                String payloadHash = sha256Hex(requestBody);

                // Build canonical request
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

                // Build string to sign
                String credentialScope = dateStamp + "/" + region + "/" + service + "/aws4_request";
                String stringToSign = "AWS4-HMAC-SHA256\n"
                        + amzDate + "\n"
                        + credentialScope + "\n"
                        + sha256Hex(canonicalRequest);

                // Calculate signature
                byte[] signingKey = getSignatureKey(secretAccessKey, dateStamp, region, service);
                String signature = HexFormat.of().formatHex(hmacSha256(signingKey, stringToSign));

                // Build Authorization header
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
                    log.debug("AWS remote MCP response ({}): {}", response.statusCode(),
                            response.body().substring(0, Math.min(200, response.body().length())));
                    return (Object) response.body();
                } else {
                    throw new ToolExecutionException(tool.getName(),
                            String.format("AWS remote MCP returned HTTP %d: %s",
                                    response.statusCode(), response.body()));
                }
            } catch (ToolExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new ToolExecutionException(tool.getName(), "Failed to call AWS remote MCP", e);
            }
        });
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

    @Override
    public boolean supports(ToolType type) {
        return type == ToolType.AWS_REMOTE;
    }

    private Map<String, Object> parseConnectionConfig(Tool tool) {
        try {
            return objectMapper.readValue(tool.getConnectionConfig(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new ToolExecutionException(tool.getName(), "Invalid connection config JSON", e);
        }
    }
}
