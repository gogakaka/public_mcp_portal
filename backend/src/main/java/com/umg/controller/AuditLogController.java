package com.umg.controller;

import com.umg.domain.enums.AuditStatus;
import com.umg.dto.AuditLogResponse;
import com.umg.dto.PageResponse;
import com.umg.service.AuditLogService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 감사 로그 조회 REST 컨트롤러.
 *
 * <p>도구 실행 이력을 다양한 필터 조건으로 조회할 수 있습니다.
 * userId, toolId, status, 날짜 범위(from, to)로 필터링 가능합니다.</p>
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * 감사 로그를 조건부로 조회합니다.
     * 모든 필터 파라미터는 선택 사항이며, 결과는 생성일시 기준 내림차순으로 정렬됩니다.
     *
     * @param userId   선택적 사용자 ID 필터
     * @param toolId   선택적 도구 ID 필터
     * @param status   선택적 실행 상태 필터 (SUCCESS, FAIL)
     * @param from     선택적 시작 날짜 (ISO-8601 형식)
     * @param to       선택적 종료 날짜 (ISO-8601 형식)
     * @param pageable 페이지네이션 파라미터
     * @return 감사 로그 페이지 응답
     */
    @GetMapping
    public ResponseEntity<PageResponse<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID toolId,
            @RequestParam(required = false) AuditStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {

        PageResponse<AuditLogResponse> response =
                auditLogService.queryAuditLogs(userId, toolId, status, from, to, pageable);
        return ResponseEntity.ok(response);
    }
}
