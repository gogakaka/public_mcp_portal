package com.umg.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * GDPR 데이터 이동권 (Data Portability) 응답 DTO.
 *
 * <p>사용자의 모든 개인 데이터를 구조화된 형태로 내보냅니다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GdprExportResponse {

    private Instant exportedAt;
    private UserData user;
    private List<ToolData> tools;
    private List<ApiKeyData> apiKeys;
    private List<PermissionData> permissions;
    private List<AuditLogData> recentAuditLogs;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserData {
        private UUID id;
        private String email;
        private String name;
        private String department;
        private String role;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolData {
        private UUID id;
        private String name;
        private String description;
        private String toolType;
        private String status;
        private Instant createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiKeyData {
        private UUID id;
        private String name;
        private Boolean isActive;
        private Instant createdAt;
        private Instant expiresAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PermissionData {
        private UUID toolId;
        private String accessLevel;
        private Instant createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditLogData {
        private UUID id;
        private String toolName;
        private String status;
        private Long executionTimeMs;
        private Instant createdAt;
    }
}
