package com.umg.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.domain.entity.AwsMcpServer;
import com.umg.domain.entity.AwsMcpSyncHistory;
import com.umg.domain.entity.Tool;
import com.umg.domain.entity.User;
import com.umg.domain.enums.*;
import com.umg.dto.AwsMcpServerDto;
import com.umg.dto.AwsMcpServerDto.*;
import com.umg.repository.AwsMcpServerRepository;
import com.umg.repository.AwsMcpSyncHistoryRepository;
import com.umg.repository.ToolRepository;
import com.umg.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AWS 원격 MCP 서버를 관리하는 서비스.
 *
 * <p>AWS에 호스팅된 MCP 서버의 CRUD, 연결 테스트, 도구 동기화 기능을 제공합니다.
 * 자격 증명은 AES-256 암호화되어 엔티티에 저장되며,
 * AWS SigV4 서명을 사용하여 원격 MCP 서버와 통신합니다.</p>
 */
@Service
public class AwsMcpServerService {

    private static final Logger log = LoggerFactory.getLogger(AwsMcpServerService.class);

    /** AWS 원격 서버 요청 타임아웃 */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /** SigV4 날짜 형식 (yyyyMMdd) */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** SigV4 타임스탬프 형식 (yyyyMMdd'T'HHmmss'Z') */
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final AwsMcpServerRepository awsMcpServerRepository;
    private final AwsMcpSyncHistoryRepository awsMcpSyncHistoryRepository;
    private final ToolRepository toolRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /**
     * 생성자 주입.
     *
     * @param awsMcpServerRepository      AWS MCP 서버 저장소
     * @param awsMcpSyncHistoryRepository 동기화 이력 저장소
     * @param toolRepository              도구 저장소
     * @param userRepository              사용자 저장소
     * @param objectMapper                JSON 직렬화/역직렬화를 위한 ObjectMapper
     */
    public AwsMcpServerService(AwsMcpServerRepository awsMcpServerRepository,
                               AwsMcpSyncHistoryRepository awsMcpSyncHistoryRepository,
                               ToolRepository toolRepository,
                               UserRepository userRepository,
                               ObjectMapper objectMapper) {
        this.awsMcpServerRepository = awsMcpServerRepository;
        this.awsMcpSyncHistoryRepository = awsMcpSyncHistoryRepository;
        this.toolRepository = toolRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ========================= CRUD 메서드 =========================

    /**
     * 새 AWS MCP 서버를 생성한다.
     *
     * <p>요청에 포함된 accessKeyId, secretAccessKey, roleArn 정보를
     * JSON으로 조합하여 {@code credentialsConfig}에 저장합니다.
     * 자격 증명은 JPA 컨버터에 의해 자동으로 AES-256 암호화됩니다.</p>
     *
     * @param request 생성 요청 DTO
     * @param userId  생성자 사용자 ID
     * @return 생성된 서버 응답 DTO
     */
    @Transactional
    public Response create(CreateRequest request, UUID userId) {
        String credentialsConfig = buildCredentialsConfig(
                request.accessKeyId(),
                request.secretAccessKey(),
                request.roleArn()
        );

        AwsMcpServer server = AwsMcpServer.builder()
                .name(request.name())
                .description(request.description())
                .endpointUrl(request.endpointUrl())
                .region(request.region())
                .service(request.service() != null ? request.service() : "execute-api")
                .authType(request.authType())
                .credentialsConfig(credentialsConfig)
                .status(DataSourceStatus.ACTIVE)
                .createdBy(userId)
                .build();

        AwsMcpServer saved = awsMcpServerRepository.save(server);
        log.info("AWS MCP 서버 '{}' 생성 완료 (생성자: {})", saved.getName(), userId);
        return toResponse(saved);
    }

    /**
     * 기존 AWS MCP 서버를 수정한다.
     *
     * <p>null이 아닌 필드만 업데이트하며, 자격 증명 관련 필드가 변경된 경우
     * {@code credentialsConfig} JSON을 재구성합니다.</p>
     *
     * @param id      서버 ID
     * @param request 수정 요청 DTO
     * @return 수정된 서버 응답 DTO
     * @throws RuntimeException 서버를 찾을 수 없는 경우
     */
    @Transactional
    public Response update(UUID id, UpdateRequest request) {
        AwsMcpServer server = awsMcpServerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AWS MCP 서버를 찾을 수 없습니다. ID: " + id));

        if (request.name() != null) {
            server.setName(request.name());
        }
        if (request.description() != null) {
            server.setDescription(request.description());
        }
        if (request.endpointUrl() != null) {
            server.setEndpointUrl(request.endpointUrl());
        }
        if (request.region() != null) {
            server.setRegion(request.region());
        }
        if (request.service() != null) {
            server.setService(request.service());
        }

        // 자격 증명 필드 중 하나라도 변경되었으면 credentialsConfig를 재구성
        boolean credentialsChanged = request.accessKeyId() != null
                || request.secretAccessKey() != null
                || request.roleArn() != null;

        if (credentialsChanged) {
            Map<String, Object> currentConfig = parseCredentialsConfig(server.getCredentialsConfig());

            String accessKeyId = request.accessKeyId() != null
                    ? request.accessKeyId() : (String) currentConfig.get("accessKeyId");
            String secretAccessKey = request.secretAccessKey() != null
                    ? request.secretAccessKey() : (String) currentConfig.get("secretAccessKey");
            String roleArn = request.roleArn() != null
                    ? request.roleArn() : (String) currentConfig.get("roleArn");

            server.setCredentialsConfig(buildCredentialsConfig(accessKeyId, secretAccessKey, roleArn));
        }

        AwsMcpServer updated = awsMcpServerRepository.save(server);
        log.info("AWS MCP 서버 '{}' 수정 완료", updated.getName());
        return toResponse(updated);
    }

    /**
     * AWS MCP 서버를 삭제한다.
     *
     * @param id 삭제할 서버 ID
     * @throws RuntimeException 서버를 찾을 수 없는 경우
     */
    @Transactional
    public void delete(UUID id) {
        AwsMcpServer server = awsMcpServerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AWS MCP 서버를 찾을 수 없습니다. ID: " + id));

        awsMcpServerRepository.delete(server);
        log.info("AWS MCP 서버 '{}' 삭제 완료", server.getName());
    }

    /**
     * ID로 AWS MCP 서버를 조회한다.
     *
     * @param id 조회할 서버 ID
     * @return 서버 응답 DTO
     * @throws RuntimeException 서버를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public Response findById(UUID id) {
        AwsMcpServer server = awsMcpServerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AWS MCP 서버를 찾을 수 없습니다. ID: " + id));
        return toResponse(server);
    }

    /**
     * 모든 AWS MCP 서버를 페이징하여 조회한다.
     *
     * @param pageable 페이징 정보
     * @return 서버 응답 DTO 페이지
     */
    @Transactional(readOnly = true)
    public Page<Response> findAll(Pageable pageable) {
        return awsMcpServerRepository.findAll(pageable)
                .map(this::toResponse);
    }

    /**
     * 상태별 AWS MCP 서버를 페이징하여 조회한다.
     *
     * @param status   필터링할 상태
     * @param pageable 페이징 정보
     * @return 서버 응답 DTO 페이지
     */
    @Transactional(readOnly = true)
    public Page<Response> findByStatus(DataSourceStatus status, Pageable pageable) {
        return awsMcpServerRepository.findByStatus(status, pageable)
                .map(this::toResponse);
    }

    // ========================= 연결 테스트 =========================

    /**
     * AWS MCP 서버에 연결 테스트를 수행한다.
     *
     * <p>저장된 자격 증명을 파싱하여 MCP "initialize" JSON-RPC 요청을 구성하고,
     * AWS SigV4 서명을 적용하여 전송합니다. 응답에서 serverInfo와
     * protocolVersion을 파싱하며, 테스트 결과에 따라 서버의
     * {@code lastHealthCheckAt}과 {@code status}를 갱신합니다.</p>
     *
     * @param id 테스트할 서버 ID
     * @return 연결 테스트 결과 DTO
     * @throws RuntimeException 서버를 찾을 수 없는 경우
     */
    @Transactional
    public ConnectionTestResult testConnection(UUID id) {
        AwsMcpServer server = awsMcpServerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AWS MCP 서버를 찾을 수 없습니다. ID: " + id));

        Map<String, Object> credentials = parseCredentialsConfig(server.getCredentialsConfig());
        String accessKeyId = (String) credentials.get("accessKeyId");
        String secretAccessKey = (String) credentials.get("secretAccessKey");

        if (accessKeyId == null || secretAccessKey == null) {
            server.setLastHealthCheckAt(Instant.now());
            server.setStatus(DataSourceStatus.ERROR);
            awsMcpServerRepository.save(server);
            return new ConnectionTestResult(false, "필수 자격 증명 누락: accessKeyId, secretAccessKey", null, null, null);
        }

        // MCP initialize JSON-RPC 페이로드 구성
        Map<String, Object> initializePayload = Map.of(
                "jsonrpc", "2.0",
                "id", "1",
                "method", "initialize",
                "params", Map.of(
                        "protocolVersion", "2024-11-05",
                        "capabilities", Map.of(),
                        "clientInfo", Map.of(
                                "name", "UMG-Gateway",
                                "version", "1.0.0"
                        )
                )
        );

        long startTime = System.currentTimeMillis();
        try {
            String requestBody = objectMapper.writeValueAsString(initializePayload);
            String responseBody = signAndSend(
                    server.getEndpointUrl(),
                    server.getRegion(),
                    server.getService(),
                    accessKeyId,
                    secretAccessKey,
                    requestBody
            );
            long responseTimeMs = System.currentTimeMillis() - startTime;

            // 응답 파싱
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) responseMap.get("result");

            String protocolVersion = null;
            String serverName = null;

            if (result != null) {
                protocolVersion = (String) result.get("protocolVersion");
                @SuppressWarnings("unchecked")
                Map<String, Object> serverInfo = (Map<String, Object>) result.get("serverInfo");
                if (serverInfo != null) {
                    serverName = (String) serverInfo.get("name");
                }
            }

            server.setLastHealthCheckAt(Instant.now());
            server.setStatus(DataSourceStatus.ACTIVE);
            awsMcpServerRepository.save(server);

            log.info("AWS MCP 서버 '{}' 연결 테스트 성공 (응답시간: {}ms)", server.getName(), responseTimeMs);
            return new ConnectionTestResult(true, "연결 성공", protocolVersion, serverName, responseTimeMs);

        } catch (Exception e) {
            long responseTimeMs = System.currentTimeMillis() - startTime;

            server.setLastHealthCheckAt(Instant.now());
            server.setStatus(DataSourceStatus.ERROR);
            awsMcpServerRepository.save(server);

            log.error("AWS MCP 서버 '{}' 연결 테스트 실패: {}", server.getName(), e.getMessage());
            return new ConnectionTestResult(false, "연결 실패: " + e.getMessage(), null, null, responseTimeMs);
        }
    }

    // ========================= 도구 동기화 =========================

    /**
     * AWS MCP 서버에서 도구를 동기화한다.
     *
     * <p>원격 MCP 서버에 "tools/list" JSON-RPC 요청을 전송하여
     * 사용 가능한 도구 목록을 가져옵니다. 발견된 각 도구에 대해:</p>
     * <ul>
     *   <li>해당 이름 + aws_server_id 조합의 도구가 없으면 새로 생성합니다
     *       (toolType=AWS_REMOTE, authType=AWS_SIGV4, status=PENDING).</li>
     *   <li>이미 존재하면 description과 inputSchema를 갱신합니다.</li>
     * </ul>
     *
     * <p>동기화 결과는 {@code AwsMcpSyncHistory}에 기록되며,
     * 서버의 {@code syncedToolCount}와 {@code lastSyncedAt}이 갱신됩니다.</p>
     *
     * @param id     동기화할 서버 ID
     * @param userId 동기화를 수행하는 사용자 ID
     * @return 동기화 결과 DTO
     * @throws RuntimeException 서버를 찾을 수 없는 경우
     */
    @Transactional
    public SyncResult syncTools(UUID id, UUID userId) {
        AwsMcpServer server = awsMcpServerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AWS MCP 서버를 찾을 수 없습니다. ID: " + id));

        Map<String, Object> credentials = parseCredentialsConfig(server.getCredentialsConfig());
        String accessKeyId = (String) credentials.get("accessKeyId");
        String secretAccessKey = (String) credentials.get("secretAccessKey");

        // MCP tools/list JSON-RPC 페이로드 구성
        Map<String, Object> toolsListPayload = Map.of(
                "jsonrpc", "2.0",
                "id", "1",
                "method", "tools/list",
                "params", Map.of()
        );

        int toolsDiscovered = 0;
        int toolsCreated = 0;
        int toolsUpdated = 0;

        try {
            String requestBody = objectMapper.writeValueAsString(toolsListPayload);
            String responseBody = signAndSend(
                    server.getEndpointUrl(),
                    server.getRegion(),
                    server.getService(),
                    accessKeyId,
                    secretAccessKey,
                    requestBody
            );

            // 응답 파싱
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) responseMap.get("result");

            if (result == null) {
                throw new RuntimeException("MCP 서버 응답에 result 필드가 없습니다.");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");

            if (tools == null) {
                tools = Collections.emptyList();
            }

            toolsDiscovered = tools.size();

            for (Map<String, Object> remoteTool : tools) {
                String toolName = (String) remoteTool.get("name");
                String toolDescription = (String) remoteTool.get("description");
                @SuppressWarnings("unchecked")
                Map<String, Object> inputSchema = (Map<String, Object>) remoteTool.get("inputSchema");

                Optional<Tool> existingTool = toolRepository.findByNameAndAwsServerId(toolName, server.getId());

                if (existingTool.isPresent()) {
                    // 기존 도구 업데이트
                    Tool tool = existingTool.get();
                    tool.setDescription(toolDescription);
                    tool.setInputSchema(inputSchema);
                    toolRepository.save(tool);
                    toolsUpdated++;
                } else {
                    // 새 도구 생성
                    Tool tool = Tool.builder()
                            .name(toolName)
                            .description(toolDescription)
                            .toolType(ToolType.AWS_REMOTE)
                            .authType(AuthType.AWS_SIGV4)
                            .status(ToolStatus.PENDING)
                            .inputSchema(inputSchema)
                            .awsServerId(server.getId())
                            .ownerId(userId)
                            .build();
                    toolRepository.save(tool);
                    toolsCreated++;
                }
            }

            // 동기화 이력 기록
            AwsMcpSyncHistory history = AwsMcpSyncHistory.builder()
                    .serverId(server.getId())
                    .status(SyncStatus.SUCCESS)
                    .toolsDiscovered(toolsDiscovered)
                    .toolsCreated(toolsCreated)
                    .toolsUpdated(toolsUpdated)
                    .build();
            awsMcpSyncHistoryRepository.save(history);

            // 서버 정보 갱신
            server.setSyncedToolCount(toolsDiscovered);
            server.setLastSyncedAt(Instant.now());
            awsMcpServerRepository.save(server);

            log.info("AWS MCP 서버 '{}' 도구 동기화 완료: 발견={}, 생성={}, 갱신={}",
                    server.getName(), toolsDiscovered, toolsCreated, toolsUpdated);

            return new SyncResult(true, "동기화 성공", toolsDiscovered, toolsCreated, toolsUpdated);

        } catch (Exception e) {
            // 실패 시 동기화 이력 기록
            AwsMcpSyncHistory history = AwsMcpSyncHistory.builder()
                    .serverId(server.getId())
                    .status(SyncStatus.FAIL)
                    .toolsDiscovered(toolsDiscovered)
                    .toolsCreated(toolsCreated)
                    .toolsUpdated(toolsUpdated)
                    .errorMessage(e.getMessage())
                    .build();
            awsMcpSyncHistoryRepository.save(history);

            log.error("AWS MCP 서버 '{}' 도구 동기화 실패: {}", server.getName(), e.getMessage());
            return new SyncResult(false, "동기화 실패: " + e.getMessage(), toolsDiscovered, toolsCreated, toolsUpdated);
        }
    }

    // ========================= 동기화 이력 조회 =========================

    /**
     * AWS MCP 서버의 동기화 이력을 페이징하여 조회한다.
     *
     * @param serverId 서버 ID
     * @param pageable 페이징 정보
     * @return 동기화 이력 응답 DTO 페이지
     */
    @Transactional(readOnly = true)
    public Page<SyncHistoryResponse> getSyncHistory(UUID serverId, Pageable pageable) {
        return awsMcpSyncHistoryRepository.findByServerIdOrderByCreatedAtDesc(serverId, pageable)
                .map(this::toSyncHistoryResponse);
    }

    // ========================= AWS SigV4 서명 헬퍼 =========================

    /**
     * SHA-256 해시를 계산하여 16진수 문자열로 반환한다.
     *
     * @param data 해시할 데이터
     * @return 소문자 16진수 SHA-256 해시 문자열
     * @throws Exception 해시 계산에 실패한 경우
     */
    private String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    /**
     * HMAC-SHA256을 계산한다.
     *
     * @param key  HMAC 키 바이트 배열
     * @param data 서명할 데이터 문자열
     * @return HMAC 결과 바이트 배열
     * @throws Exception HMAC 계산에 실패한 경우
     */
    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * AWS SigV4 서명 키를 유도한다.
     *
     * @param secretKey AWS Secret Access Key
     * @param dateStamp 날짜 (yyyyMMdd 형식)
     * @param region    AWS 리전
     * @param service   AWS 서비스명
     * @return 유도된 서명 키 바이트 배열
     * @throws Exception 키 유도에 실패한 경우
     */
    private byte[] getSignatureKey(String secretKey, String dateStamp, String region, String service)
            throws Exception {
        byte[] kDate = hmacSha256(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, service);
        return hmacSha256(kService, "aws4_request");
    }

    /**
     * AWS SigV4 서명을 적용하여 HTTP 요청을 전송한다.
     *
     * <p>요청 본문에 대한 SHA-256 해시를 계산하고, 정규 요청 문자열을 구성하여
     * SigV4 서명을 생성합니다. Authorization 헤더와 함께 POST 요청을 전송하고
     * 응답 본문을 반환합니다.</p>
     *
     * @param endpointUrl    원격 MCP 서버 엔드포인트 URL
     * @param region         AWS 리전
     * @param service        AWS 서비스명
     * @param accessKeyId    AWS Access Key ID
     * @param secretAccessKey AWS Secret Access Key
     * @param requestBody    요청 본문 (JSON 문자열)
     * @return 응답 본문 문자열
     * @throws Exception 서명 또는 전송에 실패한 경우
     */
    private String signAndSend(String endpointUrl, String region, String service,
                               String accessKeyId, String secretAccessKey,
                               String requestBody) throws Exception {
        URI uri = URI.create(endpointUrl);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String dateStamp = now.format(DATE_FORMAT);
        String amzDate = now.format(DATETIME_FORMAT);

        // 요청 본문의 SHA-256 해시 계산
        String payloadHash = sha256Hex(requestBody);

        // 정규 요청 문자열 구성
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

        // 서명 대상 문자열 구성
        String credentialScope = dateStamp + "/" + region + "/" + service + "/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest);

        // SigV4 서명 계산
        byte[] signingKey = getSignatureKey(secretAccessKey, dateStamp, region, service);
        String signature = HexFormat.of().formatHex(hmacSha256(signingKey, stringToSign));

        // Authorization 헤더 구성
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
            return response.body();
        } else {
            throw new RuntimeException(String.format(
                    "AWS 원격 MCP 서버가 HTTP %d 응답 반환: %s",
                    response.statusCode(), response.body()));
        }
    }

    // ========================= 변환 헬퍼 =========================

    /**
     * AWS MCP 서버 엔티티를 응답 DTO로 변환한다.
     *
     * <p>생성자 이름은 {@link UserRepository}를 통해 별도 조회합니다.
     * 사용자가 존재하지 않을 경우 "알 수 없음"으로 표시합니다.</p>
     *
     * @param entity 변환할 서버 엔티티
     * @return 응답 DTO
     */
    private Response toResponse(AwsMcpServer entity) {
        String creatorName = userRepository.findById(entity.getCreatedBy())
                .map(User::getName)
                .orElse("알 수 없음");

        return new Response(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getEndpointUrl(),
                entity.getRegion(),
                entity.getService(),
                entity.getAuthType(),
                entity.getStatus(),
                entity.getSyncedToolCount(),
                entity.getLastSyncedAt(),
                entity.getLastHealthCheckAt(),
                entity.getCreatedBy(),
                creatorName,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * 동기화 이력 엔티티를 응답 DTO로 변환한다.
     *
     * @param entity 변환할 동기화 이력 엔티티
     * @return 동기화 이력 응답 DTO
     */
    private SyncHistoryResponse toSyncHistoryResponse(AwsMcpSyncHistory entity) {
        return new SyncHistoryResponse(
                entity.getId(),
                entity.getServerId(),
                entity.getStatus().name(),
                entity.getToolsDiscovered(),
                entity.getToolsCreated(),
                entity.getToolsUpdated(),
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }

    /**
     * AWS 자격 증명 정보를 JSON 문자열로 조합한다.
     *
     * @param accessKeyId    AWS Access Key ID
     * @param secretAccessKey AWS Secret Access Key
     * @param roleArn        AWS IAM Role ARN (선택 사항)
     * @return JSON 형식의 자격 증명 설정 문자열
     */
    private String buildCredentialsConfig(String accessKeyId, String secretAccessKey, String roleArn) {
        try {
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("accessKeyId", accessKeyId);
            config.put("secretAccessKey", secretAccessKey);
            if (roleArn != null) {
                config.put("roleArn", roleArn);
            }
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("자격 증명 설정 JSON 생성에 실패했습니다.", e);
        }
    }

    /**
     * JSON 문자열을 Map으로 파싱한다.
     *
     * @param json 파싱할 자격 증명 JSON 문자열
     * @return 파싱된 Map
     */
    private Map<String, Object> parseCredentialsConfig(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("자격 증명 설정 JSON 파싱에 실패했습니다.", e);
        }
    }
}
