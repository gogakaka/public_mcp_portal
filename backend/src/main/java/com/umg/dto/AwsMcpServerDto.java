package com.umg.dto;

import com.umg.domain.enums.AwsAuthType;
import com.umg.domain.enums.DataSourceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * AWS MCP 서버 관련 DTO.
 */
public final class AwsMcpServerDto {

    private AwsMcpServerDto() {}

    public record CreateRequest(
            @NotBlank @Size(max = 200) String name,
            String description,
            @NotBlank @Size(max = 2000) String endpointUrl,
            @NotBlank String region,
            String service,
            @NotNull AwsAuthType authType,
            String accessKeyId,
            String secretAccessKey,
            String roleArn
    ) {}

    public record UpdateRequest(
            @Size(max = 200) String name,
            String description,
            @Size(max = 2000) String endpointUrl,
            String region,
            String service,
            String accessKeyId,
            String secretAccessKey,
            String roleArn
    ) {}

    public record Response(
            UUID id,
            String name,
            String description,
            String endpointUrl,
            String region,
            String service,
            AwsAuthType authType,
            DataSourceStatus status,
            Integer syncedToolCount,
            Instant lastSyncedAt,
            Instant lastHealthCheckAt,
            UUID createdBy,
            String creatorName,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ConnectionTestResult(
            boolean success,
            String message,
            String protocolVersion,
            String serverName,
            Long responseTimeMs
    ) {}

    public record SyncResult(
            boolean success,
            String message,
            Integer toolsDiscovered,
            Integer toolsCreated,
            Integer toolsUpdated
    ) {}

    public record SyncHistoryResponse(
            UUID id,
            UUID serverId,
            String status,
            Integer toolsDiscovered,
            Integer toolsCreated,
            Integer toolsUpdated,
            String errorMessage,
            Instant createdAt
    ) {}
}
