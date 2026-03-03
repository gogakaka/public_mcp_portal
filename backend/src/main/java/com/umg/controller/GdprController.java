package com.umg.controller;

import com.umg.dto.GdprExportResponse;
import com.umg.security.SecurityUtils;
import com.umg.service.GdprService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * GDPR 컴플라이언스 REST 컨트롤러.
 *
 * <p>데이터 이동권(데이터 내보내기)과 삭제권(계정 삭제)을 제공합니다.</p>
 */
@RestController
@RequestMapping("/api/gdpr")
@Tag(name = "GDPR", description = "GDPR 컴플라이언스 엔드포인트 (데이터 내보내기/삭제)")
public class GdprController {

    private final GdprService gdprService;
    private final SecurityUtils securityUtils;

    public GdprController(GdprService gdprService, SecurityUtils securityUtils) {
        this.gdprService = gdprService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "내 데이터 내보내기", description = "현재 사용자의 모든 개인 데이터를 JSON으로 내보냅니다.")
    @GetMapping("/export/me")
    public ResponseEntity<GdprExportResponse> exportMyData() {
        UUID userId = securityUtils.requireCurrentUserId();
        return ResponseEntity.ok(gdprService.exportUserData(userId, userId));
    }

    @Operation(summary = "특정 사용자 데이터 내보내기 (관리자 전용)")
    @GetMapping("/export/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GdprExportResponse> exportUserData(@PathVariable UUID userId) {
        UUID actorId = securityUtils.requireCurrentUserId();
        return ResponseEntity.ok(gdprService.exportUserData(userId, actorId));
    }

    @Operation(summary = "내 계정 삭제", description = "현재 사용자의 모든 데이터를 삭제합니다 (되돌릴 수 없음).")
    @DeleteMapping("/delete/me")
    public ResponseEntity<Void> deleteMyData() {
        UUID userId = securityUtils.requireCurrentUserId();
        gdprService.deleteUserData(userId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "특정 사용자 데이터 삭제 (관리자 전용)")
    @DeleteMapping("/delete/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserData(@PathVariable UUID userId) {
        UUID actorId = securityUtils.requireCurrentUserId();
        gdprService.deleteUserData(userId, actorId);
        return ResponseEntity.noContent().build();
    }
}
