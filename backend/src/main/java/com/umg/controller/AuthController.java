package com.umg.controller;

import com.umg.dto.AuthLoginRequest;
import com.umg.dto.AuthLoginResponse;
import com.umg.dto.UserDto;
import com.umg.service.AuthService;
import com.umg.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller handling authentication endpoints.
 *
 * <p>Provides login, registration, and token refresh operations.
 * These endpoints are publicly accessible (no authentication required).</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * Authenticates a user with email and password.
     *
     * @param request the login credentials
     * @return JWT tokens and user information
     */
    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        AuthLoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new user account.
     *
     * @param request the registration details
     * @return the created user information
     */
    @PostMapping("/register")
    public ResponseEntity<UserDto.Response> register(@Valid @RequestBody UserDto.CreateRequest request) {
        UserDto.Response response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Refreshes an access token using a valid refresh token.
     *
     * @param body map containing the "refreshToken" key
     * @return new JWT tokens and user information
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthLoginResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AuthLoginResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
}
