package com.umg.dto;

import com.umg.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer objects for user-related operations.
 */
public final class UserDto {

    private UserDto() {
    }

    /**
     * Request DTO for user registration.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        private String name;

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        private String password;

        private String department;
    }

    /**
     * Request DTO for updating user profile.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {

        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        private String name;

        private String department;
    }

    /**
     * Response DTO containing user information (password excluded).
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private UUID id;
        private String email;
        private String name;
        private String department;
        private UserRole role;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
