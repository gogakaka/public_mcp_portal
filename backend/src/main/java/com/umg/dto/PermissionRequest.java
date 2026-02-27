package com.umg.dto;

import com.umg.domain.enums.AccessLevel;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Request DTO for granting or revoking a permission.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionRequest {

    @NotNull(message = "Tool ID is required")
    private UUID toolId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Access level is required")
    private AccessLevel accessLevel;
}
