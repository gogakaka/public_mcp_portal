package com.umg.dto;

import com.umg.domain.enums.AccessLevel;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO containing permission information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {

    private UUID id;
    private UUID toolId;
    private String toolName;
    private UUID userId;
    private String userName;
    private AccessLevel accessLevel;
    private Instant createdAt;
}
