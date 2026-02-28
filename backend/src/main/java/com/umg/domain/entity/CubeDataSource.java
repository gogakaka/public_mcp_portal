package com.umg.domain.entity;

import com.umg.domain.enums.CubeDbType;
import com.umg.domain.enums.DataSourceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Cube.js 시맨틱 레이어가 연결할 데이터소스.
 *
 * <p>DB 연결 정보(호스트, 포트, DB명, 사용자, 비밀번호)는
 * AES-256 암호화하여 {@code connectionConfig}에 저장됩니다.</p>
 */
@Entity
@Table(name = "cube_data_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CubeDataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "db_type", nullable = false, length = 30)
    private CubeDbType dbType;

    @Column(name = "connection_config", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = com.umg.config.AesAttributeConverter.class)
    private String connectionConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DataSourceStatus status = DataSourceStatus.ACTIVE;

    @Column(name = "last_tested_at")
    private Instant lastTestedAt;

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
