package com.umg.controller;

import com.umg.domain.enums.DataSourceStatus;
import com.umg.dto.AwsMcpServerDto;
import com.umg.dto.PageResponse;
import com.umg.security.SecurityUtils;
import com.umg.service.AwsMcpServerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * AWS MCP 서버 관리 REST 컨트롤러.
 *
 * <p>AWS MCP 서버의 목록 조회(페이징, 상태 필터링), 상세 조회, 등록, 수정, 삭제와
 * 관리자 전용 연결 테스트, 도구 동기화 및 동기화 이력 조회 기능을 제공합니다.</p>
 */
@RestController
@RequestMapping("/api/aws-mcp/servers")
public class AwsMcpServerController {

    private final AwsMcpServerService awsMcpServerService;
    private final SecurityUtils securityUtils;

    public AwsMcpServerController(AwsMcpServerService awsMcpServerService,
                                   SecurityUtils securityUtils) {
        this.awsMcpServerService = awsMcpServerService;
        this.securityUtils = securityUtils;
    }

    /**
     * AWS MCP 서버 목록을 페이징하여 조회합니다.
     * status 파라미터로 상태별 필터링을 할 수 있습니다.
     *
     * @param status   선택적 상태 필터 (ACTIVE, INACTIVE, ERROR)
     * @param pageable 페이지네이션 파라미터
     * @return 서버 목록 페이지 응답
     */
    @GetMapping
    public ResponseEntity<PageResponse<AwsMcpServerDto.Response>> listServers(
            @RequestParam(required = false) DataSourceStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AwsMcpServerDto.Response> page;
        if (status != null) {
            page = awsMcpServerService.findByStatus(status, pageable);
        } else {
            page = awsMcpServerService.findAll(pageable);
        }
        return ResponseEntity.ok(PageResponse.from(page, page.getContent()));
    }

    /**
     * 특정 AWS MCP 서버의 상세 정보를 조회합니다.
     *
     * @param id 서버의 UUID
     * @return 서버 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<AwsMcpServerDto.Response> getServer(@PathVariable UUID id) {
        return ResponseEntity.ok(awsMcpServerService.findById(id));
    }

    /**
     * 새로운 AWS MCP 서버를 등록합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param request 서버 등록 요청 DTO
     * @return 등록된 서버 정보 (201 Created)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AwsMcpServerDto.Response> createServer(
            @Valid @RequestBody AwsMcpServerDto.CreateRequest request) {
        AwsMcpServerDto.Response response = awsMcpServerService.create(
                request, securityUtils.requireCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 기존 AWS MCP 서버 정보를 수정합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param id      서버의 UUID
     * @param request 서버 수정 요청 DTO
     * @return 수정된 서버 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AwsMcpServerDto.Response> updateServer(
            @PathVariable UUID id,
            @Valid @RequestBody AwsMcpServerDto.UpdateRequest request) {
        return ResponseEntity.ok(awsMcpServerService.update(id, request));
    }

    /**
     * AWS MCP 서버를 삭제합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param id 서버의 UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteServer(@PathVariable UUID id) {
        awsMcpServerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * AWS MCP 서버의 연결 상태를 테스트합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param id 테스트할 서버의 UUID
     * @return 연결 테스트 결과
     */
    @PostMapping("/{id}/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AwsMcpServerDto.ConnectionTestResult> testConnection(@PathVariable UUID id) {
        return ResponseEntity.ok(awsMcpServerService.testConnection(id));
    }

    /**
     * AWS MCP 서버에서 사용 가능한 도구를 동기화합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param id 동기화할 서버의 UUID
     * @return 도구 동기화 결과
     */
    @PostMapping("/{id}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AwsMcpServerDto.SyncResult> syncTools(@PathVariable UUID id) {
        return ResponseEntity.ok(awsMcpServerService.syncTools(id, securityUtils.requireCurrentUserId()));
    }

    /**
     * AWS MCP 서버의 도구 동기화 이력을 페이징하여 조회합니다.
     *
     * @param id       서버의 UUID
     * @param pageable 페이지네이션 파라미터
     * @return 동기화 이력 페이지 응답
     */
    @GetMapping("/{id}/sync-history")
    public ResponseEntity<PageResponse<AwsMcpServerDto.SyncHistoryResponse>> getSyncHistory(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AwsMcpServerDto.SyncHistoryResponse> page = awsMcpServerService.getSyncHistory(id, pageable);
        return ResponseEntity.ok(PageResponse.from(page, page.getContent()));
    }
}
