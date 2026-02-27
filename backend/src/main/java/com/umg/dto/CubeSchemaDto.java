package com.umg.dto;

import com.umg.domain.enums.SchemaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Cube.js 스키마 관련 DTO.
 */
public final class CubeSchemaDto {

    private CubeSchemaDto() {}

    public record CreateRequest(
            @NotNull UUID datasourceId,
            @NotBlank @Size(max = 200) String name,
            String description,
            @NotBlank String schemaDefinition
    ) {}

    public record UpdateRequest(
            @Size(max = 200) String name,
            String description,
            String schemaDefinition
    ) {}

    public record Response(
            UUID id,
            UUID datasourceId,
            String datasourceName,
            String name,
            String description,
            String schemaDefinition,
            Integer version,
            SchemaStatus status,
            UUID createdBy,
            String creatorName,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ValidationResult(
            boolean valid,
            String message
    ) {}
}
