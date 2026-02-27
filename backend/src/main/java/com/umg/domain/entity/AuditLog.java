package com.umg.domain.entity;

import com.umg.domain.enums.AuditStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit record capturing every tool execution attempt.
 *
 * <p>Audit logs are written asynchronously to avoid adding latency to the
 * tool execution path. Each record includes the full input parameters,
 * execution outcome, timing, and a distributed trace ID for correlation
 * with upstream systems.</p>
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_user_id", columnList = "user_id"),
        @Index(name = "idx_audit_logs_tool_id", columnList = "tool_id"),
        @Index(name = "idx_audit_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_logs_trace_id", columnList = "trace_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Distributed trace ID for correlating requests across services. */
    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "user_id")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "tool_id")
    private UUID toolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", insertable = false, updatable = false)
    private Tool tool;

    /** Denormalized tool name for quick querying without joins. */
    @Column(name = "tool_name", length = 255)
    private String toolName;

    /** Input parameters supplied for this execution, stored as JSONB. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_params", columnDefinition = "jsonb")
    private Map<String, Object> inputParams;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AuditStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Time taken to execute the tool in milliseconds. */
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
