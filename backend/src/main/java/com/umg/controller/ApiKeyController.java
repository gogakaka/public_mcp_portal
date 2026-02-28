package com.umg.controller;

import com.umg.dto.ApiKeyCreateRequest;
import com.umg.dto.ApiKeyResponse;
import com.umg.security.SecurityUtils;
import com.umg.service.ApiKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API 키 관리 REST 컨트롤러.
 *
 * <p>현재 인증된 사용자의 API 키 목록 조회, 생성, 비활성화 기능을 제공합니다.
 * 원본 API 키는 생성 시 한 번만 반환되며, 이후에는 조회할 수 없습니다.</p>
 */
@RestController
@RequestMapping("/api/keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final SecurityUtils securityUtils;

    public ApiKeyController(ApiKeyService apiKeyService, SecurityUtils securityUtils) {
        this.apiKeyService = apiKeyService;
        this.securityUtils = securityUtils;
    }

    /**
     * 현재 사용자의 API 키 목록을 조회합니다.
     * 원본 키 값은 포함되지 않습니다.
     *
     * @return API 키 목록
     */
    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys() {
        UUID userId = securityUtils.requireCurrentUserId();
        return ResponseEntity.ok(apiKeyService.listApiKeys(userId));
    }

    /**
     * 현재 사용자를 위한 새로운 API 키를 생성합니다.
     * 원본 키 값은 이 응답에서만 반환됩니다.
     *
     * @param request API 키 생성 요청 DTO
     * @return 생성된 API 키 정보 (원본 키 포함)
     */
    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(@Valid @RequestBody ApiKeyCreateRequest request) {
        UUID userId = securityUtils.requireCurrentUserId();
        ApiKeyResponse response = apiKeyService.createApiKey(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * API 키를 비활성화(폐기)합니다.
     * 비활성화된 키로는 더 이상 인증할 수 없습니다.
     *
     * @param id API 키의 UUID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeApiKey(@PathVariable UUID id) {
        UUID userId = securityUtils.requireCurrentUserId();
        apiKeyService.revokeApiKey(userId, id);
        return ResponseEntity.noContent().build();
    }
}
