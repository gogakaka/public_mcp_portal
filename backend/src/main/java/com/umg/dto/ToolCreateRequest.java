package com.umg.dto;

import com.umg.domain.enums.AuthType;
import com.umg.domain.enums.ToolType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;

/**
 * Request DTO for creating a new tool registration.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolCreateRequest {

    @NotBlank(message = "Tool name is required")
    @Size(max = 255, message = "Tool name must not exceed 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Tool type is required")
    private ToolType toolType;

    /**
     * JSON string containing connection configuration.
     * This will be encrypted at rest.
     */
    @NotBlank(message = "Connection config is required")
    private String connectionConfig;

    @NotNull(message = "Auth type is required")
    private AuthType authType;

    /** JSON Schema describing the expected input parameters. */
    private Map<String, Object> inputSchema;

    /** JSONPath or JQ expression for shaping the response. */
    private String responseMappingRule;

    /** Whether this tool should be publicly accessible to all users. */
    private Boolean isPublic;

    /** Whether calling this tool multiple times with the same input is safe. */
    private Boolean isIdempotent;
}
