package com.umg.controller;

import com.umg.domain.enums.SchemaStatus;
import com.umg.dto.CubeSchemaDto;
import com.umg.dto.PageResponse;
import com.umg.security.CustomUserDetails;
import com.umg.service.CubeSchemaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cube.js 스키마 관리 REST 컨트롤러.
 *
 * <p>스키마 목록 조회(페이징, 상태 및 데이터소스 필터링), 상세 조회, 생성, 수정, 삭제와
 * 스키마 활성화, 아카이브, 유효성 검증, 활성 스키마 메타 정보 조회 기능을 제공합니다.</p>
 */
@RestController
@RequestMapping("/api/cube/schemas")
public class CubeSchemaController {

    private final CubeSchemaService cubeSchemaService;

    /**
     * CubeSchemaController 생성자.
     *
     * @param cubeSchemaService 스키마 관리 서비스
     */
    public CubeSchemaController(CubeSchemaService cubeSchemaService) {
        this.cubeSchemaService = cubeSchemaService;
    }

    /**
     * 스키마 목록을 페이징하여 조회합니다.
     *
     * <p>선택적으로 상태(status) 또는 데이터소스 ID(datasourceId)로 필터링할 수 있습니다.</p>
     *
     * @param status       선택적 스키마 상태 필터 (DRAFT, ACTIVE, ARCHIVED)
     * @param datasourceId 선택적 데이터소스 ID 필터
     * @param pageable     페이지네이션 파라미터
     * @return 스키마 목록 페이지 응답
     */
    @GetMapping
    public ResponseEntity<PageResponse<CubeSchemaDto.Response>> listSchemas(
            @RequestParam(required = false) SchemaStatus status,
            @RequestParam(required = false) UUID datasourceId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CubeSchemaDto.Response> page;
        if (status != null) {
            page = cubeSchemaService.findByStatus(status, pageable);
        } else if (datasourceId != null) {
            page = cubeSchemaService.findByDatasourceId(datasourceId, pageable);
        } else {
            page = cubeSchemaService.findAll(pageable);
        }
        return ResponseEntity.ok(PageResponse.from(page, page.getContent()));
    }

    /**
     * 특정 스키마의 상세 정보를 조회합니다.
     *
     * @param id 스키마의 UUID
     * @return 스키마 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<CubeSchemaDto.Response> getSchema(@PathVariable UUID id) {
        return ResponseEntity.ok(cubeSchemaService.findById(id));
    }

    /**
     * 새로운 스키마를 생성합니다.
     *
     * <p>관리자 전용 엔드포인트입니다. 현재 인증된 사용자를 생성자로 기록합니다.</p>
     *
     * @param request 스키마 생성 요청 DTO
     * @return 생성된 스키마 정보 (201 Created)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CubeSchemaDto.Response> createSchema(
            @Valid @RequestBody CubeSchemaDto.CreateRequest request) {
        CubeSchemaDto.Response response = cubeSchemaService.create(request, getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 기존 스키마를 수정합니다.
     *
     * <p>관리자 전용 엔드포인트입니다.</p>
     *
     * @param id      스키마의 UUID
     * @param request 스키마 수정 요청 DTO
     * @return 수정된 스키마 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CubeSchemaDto.Response> updateSchema(
            @PathVariable UUID id,
            @Valid @RequestBody CubeSchemaDto.UpdateRequest request) {
        return ResponseEntity.ok(cubeSchemaService.update(id, request));
    }

    /**
     * 스키마를 삭제합니다.
     *
     * <p>관리자 전용 엔드포인트입니다.</p>
     *
     * @param id 스키마의 UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSchema(@PathVariable UUID id) {
        cubeSchemaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 스키마를 활성화합니다.
     *
     * <p>활성화 전에 스키마 정의의 유효성을 검증한 후 상태를 ACTIVE로 전환합니다.
     * 관리자 전용 엔드포인트입니다.</p>
     *
     * @param id 활성화할 스키마의 UUID
     * @return 활성화된 스키마 정보
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CubeSchemaDto.Response> activateSchema(@PathVariable UUID id) {
        return ResponseEntity.ok(cubeSchemaService.activate(id));
    }

    /**
     * 스키마를 아카이브 상태로 전환합니다.
     *
     * <p>관리자 전용 엔드포인트입니다.</p>
     *
     * @param id 아카이브할 스키마의 UUID
     * @return 아카이브된 스키마 정보
     */
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CubeSchemaDto.Response> archiveSchema(@PathVariable UUID id) {
        return ResponseEntity.ok(cubeSchemaService.archive(id));
    }

    /**
     * 스키마 정의의 유효성을 검증합니다.
     *
     * <p>스키마 정의를 JSON으로 파싱하여 필수 필드(cube, measures/dimensions)의
     * 존재 여부를 확인합니다.</p>
     *
     * @param id 검증할 스키마의 UUID
     * @return 유효성 검증 결과
     */
    @PostMapping("/{id}/validate")
    public ResponseEntity<CubeSchemaDto.ValidationResult> validateSchema(@PathVariable UUID id) {
        return ResponseEntity.ok(cubeSchemaService.validate(id));
    }

    /**
     * 활성 상태의 스키마 메타 정보를 조회합니다.
     *
     * <p>모든 ACTIVE 상태 스키마의 cube 이름, measures, dimensions 정보를 반환합니다.
     * Cube.js 시맨틱 레이어의 메타 정보 제공에 사용됩니다.</p>
     *
     * @return 활성 스키마의 메타 정보 목록
     */
    @GetMapping("/meta")
    public ResponseEntity<List<Map<String, Object>>> getMeta() {
        return ResponseEntity.ok(cubeSchemaService.getMeta());
    }

    /**
     * 현재 인증된 사용자의 UUID를 반환합니다.
     *
     * @return 현재 사용자의 UUID
     */
    private UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUserId();
    }
}
