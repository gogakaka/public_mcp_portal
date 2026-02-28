package com.umg.repository;

import com.umg.domain.entity.AwsMcpSyncHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AwsMcpSyncHistoryRepository extends JpaRepository<AwsMcpSyncHistory, UUID> {

    Page<AwsMcpSyncHistory> findByServerIdOrderByCreatedAtDesc(UUID serverId, Pageable pageable);
}
