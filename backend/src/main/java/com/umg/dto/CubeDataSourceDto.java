package com.umg.dto;

import com.umg.domain.enums.CubeDbType;
import com.umg.domain.enums.DataSourceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Cube.js 데이터소스 관련 DTO.
 */
public final class CubeDataSourceDto {

    private CubeDataSourceDto() {}

    public record CreateRequest(
            @NotBlank @Size(max = 200) String name,
            String description,
            @NotNull CubeDbType dbType,
            @NotBlank String host,
            @NotNull Integer port,
            @NotBlank String database,
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record UpdateRequest(
            @Size(max = 200) String name,
            String description,
            String host,
            Integer port,
            String database,
            String username,
            String password
    ) {}

    public record Response(
            UUID id,
            String name,
            String description,
            CubeDbType dbType,
            DataSourceStatus status,
            Instant lastTestedAt,
            UUID createdBy,
            String creatorName,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ConnectionTestResult(
            boolean success,
            String message,
            Long responseTimeMs
    ) {}
}
