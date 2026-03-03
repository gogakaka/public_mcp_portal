package com.umg.dto;

import com.umg.domain.enums.AdminAction;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 관리자 감사 로그 응답 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuditLogResponse {

    private UUID id;
    private UUID actorId;
    private String actorName;
    private AdminAction action;
    private String targetType;
    private UUID targetId;
    private String targetName;
    private Map<String, Object> details;
    private Instant createdAt;
}
