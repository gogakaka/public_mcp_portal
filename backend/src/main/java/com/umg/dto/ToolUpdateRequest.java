package com.umg.dto;

import com.umg.domain.enums.AuthType;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;

/**
 * Request DTO for updating an existing tool registration.
 * All fields are optional; only provided fields are updated.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolUpdateRequest {

    @Size(max = 255, message = "Tool name must not exceed 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    /** Updated connection configuration (will be encrypted at rest). */
    private String connectionConfig;

    private AuthType authType;

    private Map<String, Object> inputSchema;

    private String responseMappingRule;

    private Boolean isPublic;

    private Boolean isIdempotent;
}
