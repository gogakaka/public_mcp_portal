package com.umg.domain.entity;

import com.umg.domain.enums.AwsAuthType;
import com.umg.domain.enums.DataSourceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * AWS 원격 MCP 서버 레지스트리 엔티티.
 *
 * <p>AWS에 호스팅된 MCP 서버(Redshift, S3 Tables, CloudWatch 등)의
 * 연결 정보를 관리합니다. 자격 증명은 AES-256 암호화 저장됩니다.</p>
 */
@Entity
@Table(name = "aws_mcp_servers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AwsMcpServer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "endpoint_url", nullable = false, length = 2000)
    private String endpointUrl;

    @Column(name = "region", nullable = false, length = 50)
    @Builder.Default
    private String region = "us-east-1";

    @Column(name = "service", nullable = false, length = 100)
    @Builder.Default
    private String service = "execute-api";

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    @Builder.Default
    private AwsAuthType authType = AwsAuthType.IAM_KEY;

    @Column(name = "credentials_config", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = com.umg.config.AesAttributeConverter.class)
    private String credentialsConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DataSourceStatus status = DataSourceStatus.ACTIVE;

    @Column(name = "synced_tool_count", nullable = false)
    @Builder.Default
    private Integer syncedToolCount = 0;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_health_check_at")
    private Instant lastHealthCheckAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User creator;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
