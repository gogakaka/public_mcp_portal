package com.umg.dto;

import lombok.*;

/**
 * Response DTO returned after successful authentication.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthLoginResponse {

    /** JWT access token. */
    private String accessToken;

    /** JWT refresh token for obtaining new access tokens. */
    private String refreshToken;

    /** Access token type, typically "Bearer". */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Access token expiration time in seconds. */
    private Long expiresIn;

    /** Authenticated user information. */
    private UserDto.Response user;
}
