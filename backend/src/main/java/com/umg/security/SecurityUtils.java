package com.umg.security;

import com.umg.domain.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Helper component for accessing the currently authenticated user's identity
 * from the Spring Security context.
 */
@Component
public class SecurityUtils {

    /**
     * Returns the authenticated user's ID from the security context.
     *
     * @return an Optional containing the user's UUID, or empty if not authenticated
     */
    public Optional<UUID> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        String principal = authentication.getName();
        try {
            return Optional.of(UUID.fromString(principal));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the authenticated user's ID, throwing an exception if not authenticated.
     *
     * @return the current user's UUID
     * @throws IllegalStateException if no authenticated user is found
     */
    public UUID requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("No authenticated user found in security context"));
    }

    /**
     * Checks whether the current user has the ADMIN role.
     *
     * @return {@code true} if the current user is an admin
     */
    public boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_" + UserRole.ADMIN.name()));
    }
}
