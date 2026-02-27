package com.umg.domain.entity;

import com.umg.domain.enums.AuthType;
import com.umg.domain.enums.ToolStatus;
import com.umg.domain.enums.ToolType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a registered MCP tool in the gateway.
 *
 * <p>Each tool describes a single capability that can be invoked by agents or
 * users. Tools follow a Maker-Checker workflow: they are created with
 * {@link ToolStatus#PENDING} status and must be approved by an admin before
 * they become available for execution.</p>
 *
 * <p>The {@code connectionConfig} field stores backend-specific configuration
 * (URLs, credentials, regions) and is transparently encrypted at rest using
 * AES-256 via a JPA {@link com.umg.config.AesAttributeConverter}.</p>
 */
@Entity
@Table(name = "tools", indexes = {
        @Index(name = "idx_tools_status", columnList = "status"),
        @Index(name = "idx_tools_owner_id", columnList = "owner_id"),
        @Index(name = "idx_tools_is_public", columnList = "is_public"),
        @Index(name = "idx_tools_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tool {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_type", nullable = false, length = 20)
    private ToolType toolType;

    /**
     * Backend connection configuration stored as encrypted JSONB.
     * Contains URLs, tokens, region settings, etc.
     */
    @Column(name = "connection_config", columnDefinition = "TEXT")
    @Convert(converter = com.umg.config.AesAttributeConverter.class)
    private String connectionConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    @Builder.Default
    private AuthType authType = AuthType.NONE;

    /** JSON Schema defining the expected input parameters for this tool. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_schema", columnDefinition = "jsonb")
    private Map<String, Object> inputSchema;

    /**
     * JSONPath or JQ expression used to transform the raw backend response
     * before returning it to the caller.
     */
    @Column(name = "response_mapping_rule", columnDefinition = "TEXT")
    private String responseMappingRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ToolStatus status = ToolStatus.PENDING;

    /** Whether the tool is available to all authenticated users without explicit permission. */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    /** Whether calling this tool multiple times with the same input is safe. */
    @Column(name = "is_idempotent", nullable = false)
    @Builder.Default
    private Boolean isIdempotent = false;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private User owner;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
