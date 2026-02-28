package com.umg.repository;

import com.umg.domain.entity.AuditLog;
import com.umg.domain.enums.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AuditLog} entities.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Finds audit logs for a specific user ordered by creation date descending.
     *
     * @param userId   the user's ID
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Finds audit logs for a specific tool ordered by creation date descending.
     *
     * @param toolId   the tool's ID
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLog> findByToolIdOrderByCreatedAtDesc(UUID toolId, Pageable pageable);

    /**
     * Finds audit logs within a date range.
     *
     * @param start    the start of the range (inclusive)
     * @param end      the end of the range (exclusive)
     * @param pageable pagination information
     * @return a page of audit logs
     */
    Page<AuditLog> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);

    /**
     * Finds audit logs with combined filters.
     *
     * @param userId   optional user ID filter
     * @param toolId   optional tool ID filter
     * @param status   optional status filter
     * @param start    optional start date filter
     * @param end      optional end date filter
     * @param pageable pagination information
     * @return a page of matching audit logs
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:userId IS NULL OR a.userId = :userId)
              AND (:toolId IS NULL OR a.toolId = :toolId)
              AND (:status IS NULL OR a.status = :status)
              AND (:start IS NULL OR a.createdAt >= :start)
              AND (:end IS NULL OR a.createdAt <= :end)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findWithFilters(
            @Param("userId") UUID userId,
            @Param("toolId") UUID toolId,
            @Param("status") AuditStatus status,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );

    /**
     * Counts audit logs by status (for dashboard statistics).
     *
     * @param status the status to count
     * @return the number of audit logs with the given status
     */
    long countByStatus(AuditStatus status);

    /**
     * Counts audit logs created within a date range (for dashboard statistics).
     *
     * @param start the start of the range
     * @param end   the end of the range
     * @return the count
     */
    long countByCreatedAtBetween(Instant start, Instant end);
}
