package com.umg.repository;

import com.umg.domain.entity.AwsMcpServer;
import com.umg.domain.enums.DataSourceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AwsMcpServerRepository extends JpaRepository<AwsMcpServer, UUID> {

    Optional<AwsMcpServer> findByName(String name);

    boolean existsByName(String name);

    Page<AwsMcpServer> findByStatus(DataSourceStatus status, Pageable pageable);

    List<AwsMcpServer> findByStatusOrderByNameAsc(DataSourceStatus status);

    long countByStatus(DataSourceStatus status);
}
