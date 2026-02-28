package com.umg.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO containing API key information.
 *
 * <p>The {@code rawKey} field is only populated when the key is first created.
 * Subsequent reads will have this field set to {@code null}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyResponse {

    private UUID id;
    private String name;

    /** The raw API key, only returned at creation time. */
    private String rawKey;

    private Integer rateLimitPerMin;
    private Instant expiresAt;
    private Boolean isActive;
    private Instant createdAt;
}
