package com.umg.repository;

import com.umg.domain.entity.CubeSchema;
import com.umg.domain.enums.SchemaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CubeSchemaRepository extends JpaRepository<CubeSchema, UUID> {

    Optional<CubeSchema> findByName(String name);

    boolean existsByName(String name);

    Page<CubeSchema> findByDatasourceId(UUID datasourceId, Pageable pageable);

    Page<CubeSchema> findByStatus(SchemaStatus status, Pageable pageable);

    List<CubeSchema> findByDatasourceIdAndStatus(UUID datasourceId, SchemaStatus status);

    List<CubeSchema> findByStatusOrderByNameAsc(SchemaStatus status);

    long countByDatasourceId(UUID datasourceId);
}
