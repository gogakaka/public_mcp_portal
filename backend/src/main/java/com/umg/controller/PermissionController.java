package com.umg.controller;

import com.umg.dto.PermissionRequest;
import com.umg.dto.PermissionResponse;
import com.umg.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 권한 관리 REST 컨트롤러.
 *
 * <p>사용자에게 도구 접근 권한을 부여하거나 취소하는 기능을 제공합니다.
 * 권한 부여 및 취소는 관리자만 수행할 수 있으며,
 * 권한 목록 조회는 인증된 사용자 모두 가능합니다.</p>
 */
@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 권한 목록을 조회합니다.
     * userId 또는 toolId로 필터링할 수 있으며, 최소 하나의 필터가 필요합니다.
     *
     * @param userId 선택적 사용자 ID 필터
     * @param toolId 선택적 도구 ID 필터
     * @return 권한 목록
     */
    @GetMapping
    public ResponseEntity<List<PermissionResponse>> listPermissions(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID toolId) {
        if (userId != null) {
            return ResponseEntity.ok(permissionService.listPermissionsByUser(userId));
        } else if (toolId != null) {
            return ResponseEntity.ok(permissionService.listPermissionsByTool(toolId));
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * 사용자에게 특정 도구에 대한 권한을 부여합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param request 권한 부여 요청 DTO (userId, toolId, accessLevel 포함)
     * @return 생성된 권한 정보
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PermissionResponse> grantPermission(@Valid @RequestBody PermissionRequest request) {
        PermissionResponse response = permissionService.grantPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 권한을 취소(삭제)합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param id 권한의 UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revokePermission(@PathVariable UUID id) {
        permissionService.revokePermission(id);
        return ResponseEntity.noContent().build();
    }
}
