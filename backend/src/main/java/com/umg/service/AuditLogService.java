package com.umg.service;

import com.umg.domain.entity.AuditLog;
import com.umg.domain.entity.User;
import com.umg.domain.enums.AuditStatus;
import com.umg.dto.AuditLogResponse;
import com.umg.dto.PageResponse;
import com.umg.repository.AuditLogRepository;
import com.umg.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service handling asynchronous audit log writing and querying.
 *
 * <p>Audit logs are written asynchronously to avoid adding latency to the
 * tool execution path. The service captures the MDC trace ID at call time
 * to ensure logs are correlated with the originating request.</p>
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Writes an audit log entry asynchronously.
     *
     * @param userId          the user who invoked the tool
     * @param toolId          the tool that was invoked
     * @param toolName        the tool name (denormalized)
     * @param inputParams     the input parameters
     * @param status          the execution outcome
     * @param errorMessage    optional error message if the execution failed
     * @param executionTimeMs the execution time in milliseconds
     */
    @Async("asyncExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeAuditLog(UUID userId, UUID toolId, String toolName,
                              Map<String, Object> inputParams, AuditStatus status,
                              String errorMessage, Long executionTimeMs) {
        String traceId = MDC.get("traceId");

        try {
            AuditLog auditLog = AuditLog.builder()
                    .traceId(traceId)
                    .userId(userId)
                    .toolId(toolId)
                    .toolName(toolName)
                    .inputParams(inputParams)
                    .status(status)
                    .errorMessage(errorMessage)
                    .executionTimeMs(executionTimeMs)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log written for tool '{}': status={}, time={}ms",
                    toolName, status, executionTimeMs);
        } catch (Exception e) {
            log.error("Failed to write audit log for tool '{}': {}", toolName, e.getMessage(), e);
        }
    }

    /**
     * Queries audit logs with optional filters.
     *
     * @param userId   optional user ID filter
     * @param toolId   optional tool ID filter
     * @param status   optional status filter
     * @param start    optional start date filter
     * @param end      optional end date filter
     * @param pageable pagination information
     * @return a page of audit log responses
     */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> queryAuditLogs(UUID userId, UUID toolId,
                                                          AuditStatus status,
                                                          Instant start, Instant end,
                                                          Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findWithFilters(userId, toolId, status, start, end, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    /**
     * Counts audit logs by status (for dashboard statistics).
     *
     * @param status the status to count
     * @return the count
     */
    @Transactional(readOnly = true)
    public long countByStatus(AuditStatus status) {
        return auditLogRepository.countByStatus(status);
    }

    /**
     * Counts audit logs within a date range.
     *
     * @param start the start of the range
     * @param end   the end of the range
     * @return the count
     */
    @Transactional(readOnly = true)
    public long countByDateRange(Instant start, Instant end) {
        return auditLogRepository.countByCreatedAtBetween(start, end);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        String userName = null;
        if (auditLog.getUserId() != null) {
            userName = userRepository.findById(auditLog.getUserId())
                    .map(User::getName)
                    .orElse(null);
        }

        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .traceId(auditLog.getTraceId())
                .userId(auditLog.getUserId())
                .userName(userName)
                .toolId(auditLog.getToolId())
                .toolName(auditLog.getToolName())
                .inputParams(auditLog.getInputParams())
                .status(auditLog.getStatus())
                .errorMessage(auditLog.getErrorMessage())
                .executionTimeMs(auditLog.getExecutionTimeMs())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
