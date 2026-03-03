package com.umg.repository;

import com.umg.domain.entity.AdminAuditLog;
import com.umg.domain.enums.AdminAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * 관리자 감사 로그 저장소.
 */
@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {

    Page<AdminAuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId, Pageable pageable);

    Page<AdminAuditLog> findByActionOrderByCreatedAtDesc(AdminAction action, Pageable pageable);

    @Query("""
            SELECT a FROM AdminAuditLog a
            WHERE (:actorId IS NULL OR a.actorId = :actorId)
              AND (:action IS NULL OR a.action = :action)
              AND (:start IS NULL OR a.createdAt >= :start)
              AND (:end IS NULL OR a.createdAt <= :end)
            ORDER BY a.createdAt DESC
            """)
    Page<AdminAuditLog> findWithFilters(
            @Param("actorId") UUID actorId,
            @Param("action") AdminAction action,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );
}
