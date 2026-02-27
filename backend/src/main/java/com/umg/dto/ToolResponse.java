package com.umg.dto;

import com.umg.domain.enums.AuthType;
import com.umg.domain.enums.ToolStatus;
import com.umg.domain.enums.ToolType;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO containing tool information.
 * Connection configuration is excluded for security.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolResponse {

    private UUID id;
    private String name;
    private String description;
    private ToolType toolType;
    private AuthType authType;
    private Map<String, Object> inputSchema;
    private String responseMappingRule;
    private ToolStatus status;
    private Boolean isPublic;
    private Boolean isIdempotent;
    private UUID ownerId;
    private String ownerName;
    private Instant createdAt;
    private Instant updatedAt;
}
