package com.umg.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

/**
 * Request DTO for creating a new API key.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyCreateRequest {

    @NotBlank(message = "Key name is required")
    @Size(max = 128, message = "Key name must not exceed 128 characters")
    private String name;

    @Min(value = 1, message = "Rate limit must be at least 1")
    @Max(value = 10000, message = "Rate limit must not exceed 10000")
    private Integer rateLimitPerMin;

    /** Optional expiration timestamp. Null means the key never expires. */
    private Instant expiresAt;
}
