package com.umg.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an API key issued to a {@link User} for programmatic access.
 *
 * <p>The raw key is returned exactly once at creation time. Only the SHA-256
 * hash ({@code keyHash}) is persisted so that the original key cannot be
 * recovered from the database.</p>
 */
@Entity
@Table(name = "api_keys", indexes = {
        @Index(name = "idx_api_keys_key_hash", columnList = "key_hash", unique = true),
        @Index(name = "idx_api_keys_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /** SHA-256 hash of the raw API key. */
    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** Maximum number of requests allowed per minute for this key. */
    @Column(name = "rate_limit_per_min", nullable = false)
    @Builder.Default
    private Integer rateLimitPerMin = 60;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
