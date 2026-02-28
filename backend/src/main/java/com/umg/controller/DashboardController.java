package com.umg.controller;

import com.umg.domain.enums.AuditStatus;
import com.umg.domain.enums.ToolStatus;
import com.umg.dto.DashboardStatsResponse;
import com.umg.repository.ApiKeyRepository;
import com.umg.repository.ToolRepository;
import com.umg.repository.UserRepository;
import com.umg.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * 대시보드 통계 REST 컨트롤러.
 *
 * <p>관리자 대시보드에 표시할 집계 통계 정보를 제공합니다.
 * 사용자 수, 도구 수, 금일 실행 현황, API 키 수 등의 정보를 포함합니다.</p>
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final UserRepository userRepository;
    private final ToolRepository toolRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final AuditLogService auditLogService;

    public DashboardController(UserRepository userRepository,
                               ToolRepository toolRepository,
                               ApiKeyRepository apiKeyRepository,
                               AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.toolRepository = toolRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * 대시보드 통계 정보를 조회합니다.
     *
     * <p>전체 사용자 수, 도구 상태별 수, 금일 실행 성공/실패 건수,
     * 전체 API 키 수를 집계하여 반환합니다.</p>
     *
     * @return 대시보드 통계 응답
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        /* 금일 시작 및 종료 시각 (UTC 기준) */
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant todayEnd = todayStart.plusSeconds(86400);

        long totalExecutionsToday = auditLogService.countByDateRange(todayStart, todayEnd);

        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalTools(toolRepository.count())
                .approvedTools(toolRepository.countByStatus(ToolStatus.APPROVED))
                .pendingTools(toolRepository.countByStatus(ToolStatus.PENDING))
                .rejectedTools(toolRepository.countByStatus(ToolStatus.REJECTED))
                .totalExecutionsToday(totalExecutionsToday)
                .successfulExecutionsToday(auditLogService.countByStatus(AuditStatus.SUCCESS))
                .failedExecutionsToday(auditLogService.countByStatus(AuditStatus.FAIL))
                .totalApiKeys(apiKeyRepository.count())
                .build();

        return ResponseEntity.ok(stats);
    }
}
