package com.umg.dto;

import com.umg.domain.enums.AuditStatus;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for audit log entries.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private UUID id;
    private String traceId;
    private UUID userId;
    private String userName;
    private UUID toolId;
    private String toolName;
    private Map<String, Object> inputParams;
    private AuditStatus status;
    private String errorMessage;
    private Long executionTimeMs;
    private Instant createdAt;
}
