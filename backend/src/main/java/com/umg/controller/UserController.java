package com.umg.controller;

import com.umg.dto.PageResponse;
import com.umg.dto.UserDto;
import com.umg.security.SecurityUtils;
import com.umg.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 프로필 및 관리 REST 컨트롤러.
 *
 * <p>현재 인증된 사용자의 프로필 조회/수정과 관리자 전용
 * 사용자 목록 조회 기능을 제공합니다.</p>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    public UserController(UserService userService, SecurityUtils securityUtils) {
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    /**
     * 현재 인증된 사용자의 프로필 정보를 조회합니다.
     *
     * @return 현재 사용자 프로필 정보
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto.Response> getCurrentUser() {
        var userId = securityUtils.requireCurrentUserId();
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    /**
     * 현재 인증된 사용자의 프로필 정보를 업데이트합니다.
     *
     * @param request 프로필 업데이트 요청 DTO
     * @return 업데이트된 사용자 프로필 정보
     */
    @PutMapping("/me")
    public ResponseEntity<UserDto.Response> updateCurrentUser(@Valid @RequestBody UserDto.UpdateRequest request) {
        var userId = securityUtils.requireCurrentUserId();
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    /**
     * 전체 사용자 목록을 페이징하여 조회합니다.
     * 관리자 전용 엔드포인트입니다.
     *
     * @param pageable 페이지네이션 파라미터
     * @return 사용자 목록 페이지 응답
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserDto.Response>> listUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }
}
