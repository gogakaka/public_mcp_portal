package com.umg.domain.entity;

import com.umg.domain.enums.SchemaStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Cube.js 스키마(모델) 정의.
 *
 * <p>measures, dimensions, joins 등의 Cube.js 모델 정보를
 * YAML/JSON 텍스트로 저장합니다. 버전 관리를 통해 롤백을 지원합니다.</p>
 */
@Entity
@Table(name = "cube_schemas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CubeSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "datasource_id", nullable = false)
    private UUID datasourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasource_id", insertable = false, updatable = false)
    private CubeDataSource datasource;

    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "schema_definition", nullable = false, columnDefinition = "TEXT")
    private String schemaDefinition;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SchemaStatus status = SchemaStatus.DRAFT;

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
