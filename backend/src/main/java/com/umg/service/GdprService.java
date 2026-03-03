package com.umg.service;

import com.umg.domain.entity.*;
import com.umg.domain.enums.AdminAction;
import com.umg.dto.GdprExportResponse;
import com.umg.exception.ResourceNotFoundException;
import com.umg.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GDPR 컴플라이언스 서비스.
 *
 * <p>데이터 이동권(Right to Data Portability)과 삭제권(Right to Erasure)을 구현합니다.</p>
 */
@Service
public class GdprService {

    private static final Logger log = LoggerFactory.getLogger(GdprService.class);

    private final UserRepository userRepository;
    private final ToolRepository toolRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdminAuditService adminAuditService;

    public GdprService(UserRepository userRepository, ToolRepository toolRepository,
                       ApiKeyRepository apiKeyRepository, PermissionRepository permissionRepository,
                       AuditLogRepository auditLogRepository, AdminAuditService adminAuditService) {
        this.userRepository = userRepository;
        this.toolRepository = toolRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.permissionRepository = permissionRepository;
        this.auditLogRepository = auditLogRepository;
        this.adminAuditService = adminAuditService;
    }

    /**
     * 사용자의 모든 개인 데이터를 구조화된 형태로 내보냅니다 (데이터 이동권).
     *
     * @param userId 사용자 ID
     * @param actorId 요청을 수행하는 사용자 ID (관리자 또는 본인)
     * @return 내보내기 결과
     */
    @Transactional(readOnly = true)
    public GdprExportResponse exportUserData(UUID userId, UUID actorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        List<GdprExportResponse.ToolData> tools = toolRepository.findAll().stream()
                .filter(t -> userId.equals(t.getOwnerId()))
                .map(t -> GdprExportResponse.ToolData.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .description(t.getDescription())
                        .toolType(t.getToolType().name())
                        .status(t.getStatus().name())
                        .createdAt(t.getCreatedAt())
                        .build())
                .toList();

        List<GdprExportResponse.ApiKeyData> apiKeys = apiKeyRepository.findByUserId(userId).stream()
                .map(k -> GdprExportResponse.ApiKeyData.builder()
                        .id(k.getId())
                        .name(k.getName())
                        .isActive(k.getIsActive())
                        .createdAt(k.getCreatedAt())
                        .expiresAt(k.getExpiresAt())
                        .build())
                .toList();

        List<GdprExportResponse.PermissionData> permissions = permissionRepository.findByUserId(userId).stream()
                .map(p -> GdprExportResponse.PermissionData.builder()
                        .toolId(p.getToolId())
                        .accessLevel(p.getAccessLevel().name())
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();

        List<GdprExportResponse.AuditLogData> auditLogs = auditLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId,
                        PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream()
                .map(a -> GdprExportResponse.AuditLogData.builder()
                        .id(a.getId())
                        .toolName(a.getToolName())
                        .status(a.getStatus().name())
                        .executionTimeMs(a.getExecutionTimeMs())
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList();

        adminAuditService.log(actorId, AdminAction.USER_DATA_EXPORT, "User",
                userId, user.getEmail(), Map.of("exportedFields", "profile,tools,apiKeys,permissions,auditLogs"));

        log.info("GDPR data export completed for user: {} ({})", user.getEmail(), userId);

        return GdprExportResponse.builder()
                .exportedAt(Instant.now())
                .user(GdprExportResponse.UserData.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .department(user.getDepartment())
                        .role(user.getRole().name())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build())
                .tools(tools)
                .apiKeys(apiKeys)
                .permissions(permissions)
                .recentAuditLogs(auditLogs)
                .build();
    }

    /**
     * 사용자의 개인 데이터를 삭제합니다 (삭제권 / Right to Erasure).
     *
     * <p>감사 로그의 사용자 참조는 NULL로 설정되며 (FK ON DELETE SET NULL),
     * 도구, API 키, 권한은 cascade로 삭제됩니다.</p>
     *
     * @param userId  삭제할 사용자 ID
     * @param actorId 요청을 수행하는 관리자 ID
     */
    @Transactional
    public void deleteUserData(UUID userId, UUID actorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        String userEmail = user.getEmail();

        // 권한 삭제 (cascade로 처리되지만 명시적으로 수행)
        permissionRepository.findByUserId(userId).forEach(permissionRepository::delete);

        // API 키 삭제
        apiKeyRepository.findByUserId(userId).forEach(apiKeyRepository::delete);

        // 사용자 삭제 (audit_logs FK는 ON DELETE SET NULL로 자동 처리)
        userRepository.delete(user);

        adminAuditService.log(actorId, AdminAction.USER_DELETE, "User",
                userId, userEmail, Map.of("reason", "GDPR Right to Erasure"));

        log.info("GDPR user data deletion completed for: {} ({})", userEmail, userId);
    }
}
