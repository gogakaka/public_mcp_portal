package com.umg.controller;

import com.umg.domain.enums.ToolStatus;
import com.umg.dto.PageResponse;
import com.umg.dto.ToolCreateRequest;
import com.umg.dto.ToolResponse;
import com.umg.dto.ToolUpdateRequest;
import com.umg.service.ToolService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 도구 CRUD 및 승인 워크플로우 REST 컨트롤러.
 *
 * <p>도구 목록 조회(페이징, 필터링), 상세 조회, 생성, 수정, 삭제와
 * 관리자 전용 승인/거부 기능을 제공합니다.</p>
 */
@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    /**
     * 도구 목록을 페이징하여 조회합니다.
     * status, type, search 파라미터로 필터링할 수 있습니다.
     *
     * @param status   선택적 상태 필터 (PENDING, APPROVED, REJECTED)
     * @param pageable 페이지네이션 파라미터
     * @return 도구 목록 페이지 응답
     */
    @GetMapping
    public ResponseEntity<PageResponse<ToolResponse>> listTools(
            @RequestParam(required = false) ToolStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(toolService.listByStatus(status, pageable));
        }
        return ResponseEntity.ok(toolService.listAccessibleTools(pageable));
    }

    /**
     * 특정 도구의 상세 정보를 조회합니다.
     *
     * @param id 도구의 UUID
     * @return 도구 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<ToolResponse> getTool(@PathVariable UUID id) {
        return ResponseEntity.ok(toolService.getToolById(id));
    }

    /**
     * 새로운 도구를 PENDING 상태로 생성합니다.
     * 관리자의 승인 후에야 사용 가능합니다.
     *
     * @param request 도구 생성 요청 DTO
     * @return 생성된 도구 정보
     */
    @PostMapping
    public ResponseEntity<ToolResponse> createTool(@Valid @RequestBody ToolCreateRequest request) {
        ToolResponse response = toolService.createTool(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 기존 도구를 업데이트합니다.
     * 업데이트 시 상태가 PENDING으로 재설정되어 재승인이 필요합니다.
     *
     * @param id      도구의 UUID
     * @param request 도구 수정 요청 DTO
     * @return 업데이트된 도구 정보
     */
    @PutMapping("/{id}")
    public ResponseEntity<ToolResponse> updateTool(
            @PathVariable UUID id,
            @Valid @RequestBody ToolUpdateRequest request) {
        return ResponseEntity.ok(toolService.updateTool(id, request));
    }

    /**
     * 도구를 삭제합니다.
     * 도구 소유자 또는 관리자만 삭제할 수 있습니다.
     *
     * @param id 도구의 UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTool(@PathVariable UUID id) {
        toolService.deleteTool(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PENDING 상태의 도구를 승인합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param id 도구의 UUID
     * @return 승인된 도구 정보
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ToolResponse> approveTool(@PathVariable UUID id) {
        return ResponseEntity.ok(toolService.approveTool(id));
    }

    /**
     * PENDING 상태의 도구를 거부합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param id 도구의 UUID
     * @return 거부된 도구 정보
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ToolResponse> rejectTool(@PathVariable UUID id) {
        return ResponseEntity.ok(toolService.rejectTool(id));
    }
}
