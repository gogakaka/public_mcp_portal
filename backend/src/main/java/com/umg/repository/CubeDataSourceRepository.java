package com.umg.repository;

import com.umg.domain.entity.CubeDataSource;
import com.umg.domain.enums.CubeDbType;
import com.umg.domain.enums.DataSourceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CubeDataSourceRepository extends JpaRepository<CubeDataSource, UUID> {

    Optional<CubeDataSource> findByName(String name);

    boolean existsByName(String name);

    Page<CubeDataSource> findByStatus(DataSourceStatus status, Pageable pageable);

    Page<CubeDataSource> findByDbType(CubeDbType dbType, Pageable pageable);

    List<CubeDataSource> findByStatusOrderByNameAsc(DataSourceStatus status);

    long countByStatus(DataSourceStatus status);
}
