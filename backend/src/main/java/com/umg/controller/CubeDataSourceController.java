package com.umg.controller;

import com.umg.domain.enums.DataSourceStatus;
import com.umg.dto.CubeDataSourceDto;
import com.umg.dto.PageResponse;
import com.umg.security.CustomUserDetails;
import com.umg.service.CubeDataSourceService;
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
 * Cube.js 데이터소스 관리 REST 컨트롤러.
 *
 * <p>데이터소스 목록 조회(페이징, 상태 필터링), 상세 조회, 생성, 수정, 삭제와
 * 관리자 전용 연결 테스트 및 테이블 인트로스펙션 기능을 제공합니다.</p>
 */
@RestController
@RequestMapping("/api/cube/datasources")
public class CubeDataSourceController {

    private final CubeDataSourceService cubeDataSourceService;

    public CubeDataSourceController(CubeDataSourceService cubeDataSourceService) {
        this.cubeDataSourceService = cubeDataSourceService;
    }

    /**
     * 데이터소스 목록을 페이징하여 조회합니다.
     * status 파라미터로 상태별 필터링을 할 수 있습니다.
     *
     * @param status   선택적 상태 필터 (ACTIVE, INACTIVE, ERROR)
     * @param pageable 페이지네이션 파라미터
     * @return 데이터소스 목록 페이지 응답
     */
    @GetMapping
    public ResponseEntity<PageResponse<CubeDataSourceDto.Response>> listDataSources(
            @RequestParam(required = false) DataSourceStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CubeDataSourceDto.Response> page;
        if (status != null) {
            page = cubeDataSourceService.findByStatus(status, pageable);
        } else {
            page = cubeDataSourceService.findAll(pageable);
        }
        return ResponseEntity.ok(PageResponse.from(page, page.getContent()));
    }

    /**
     * 특정 데이터소스의 상세 정보를 조회합니다.
     *
     * @param id 데이터소스의 UUID
     * @return 데이터소스 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<CubeDataSourceDto.Response> getDataSource(@PathVariable UUID id) {
        return ResponseEntity.ok(cubeDataSourceService.findById(id));
    }

    /**
     * 새로운 데이터소스를 생성합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param request 데이터소스 생성 요청 DTO
     * @return 생성된 데이터소스 정보 (201 Created)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CubeDataSourceDto.Response> createDataSource(
            @Valid @RequestBody CubeDataSourceDto.CreateRequest request) {
        CubeDataSourceDto.Response response = cubeDataSourceService.create(request, getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 기존 데이터소스를 수정합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param id      데이터소스의 UUID
     * @param request 데이터소스 수정 요청 DTO
     * @return 수정된 데이터소스 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CubeDataSourceDto.Response> updateDataSource(
            @PathVariable UUID id,
            @Valid @RequestBody CubeDataSourceDto.UpdateRequest request) {
        return ResponseEntity.ok(cubeDataSourceService.update(id, request));
    }

    /**
     * 데이터소스를 삭제합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param id 데이터소스의 UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDataSource(@PathVariable UUID id) {
        cubeDataSourceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 데이터소스의 DB 연결을 테스트합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param id 테스트할 데이터소스의 UUID
     * @return 연결 테스트 결과
     */
    @PostMapping("/{id}/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CubeDataSourceDto.ConnectionTestResult> testConnection(@PathVariable UUID id) {
        return ResponseEntity.ok(cubeDataSourceService.testConnection(id));
    }

    /**
     * 데이터소스에 연결하여 사용 가능한 테이블과 컬럼 정보를 조회합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param id 인트로스펙션할 데이터소스의 UUID
     * @return 테이블명을 키로, 컬럼 정보 리스트를 값으로 가지는 Map
     */
    @GetMapping("/{id}/tables")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> introspectTables(@PathVariable UUID id) {
        return ResponseEntity.ok(cubeDataSourceService.introspectTables(id));
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
