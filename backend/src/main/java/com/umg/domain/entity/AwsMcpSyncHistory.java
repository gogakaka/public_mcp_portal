package com.umg.domain.entity;

import com.umg.domain.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * AWS MCP 서버 도구 동기화 이력.
 */
@Entity
@Table(name = "aws_mcp_sync_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AwsMcpSyncHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "server_id", nullable = false)
    private UUID serverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", insertable = false, updatable = false)
    private AwsMcpServer server;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SyncStatus status;

    @Column(name = "tools_discovered", nullable = false)
    @Builder.Default
    private Integer toolsDiscovered = 0;

    @Column(name = "tools_created", nullable = false)
    @Builder.Default
    private Integer toolsCreated = 0;

    @Column(name = "tools_updated", nullable = false)
    @Builder.Default
    private Integer toolsUpdated = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
