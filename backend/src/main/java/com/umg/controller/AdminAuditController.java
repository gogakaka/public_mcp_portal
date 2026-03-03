package com.umg.controller;

import com.umg.domain.enums.AdminAction;
import com.umg.dto.AdminAuditLogResponse;
import com.umg.dto.PageResponse;
import com.umg.service.AdminAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 관리자 감사 로그 조회 컨트롤러.
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Audit", description = "관리자 행위 감사 로그 조회 (관리자 전용)")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @Operation(summary = "관리자 감사 로그 조회", description = "관리자 행위(도구 승인/거부, 권한 변경 등)의 감사 로그를 조회합니다.")
    @GetMapping
    public ResponseEntity<PageResponse<AdminAuditLogResponse>> getAdminAuditLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) AdminAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(adminAuditService.query(actorId, action, from, to, pageable));
    }
}
