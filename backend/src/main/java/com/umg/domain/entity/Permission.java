package com.umg.domain.entity;

import com.umg.domain.enums.AccessLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an explicit permission grant giving a {@link User} a specific
 * {@link AccessLevel} on a {@link Tool}.
 *
 * <p>Permissions are checked in addition to the tool's {@code isPublic} flag.
 * A user has access to a tool if the tool is public <em>or</em> if an
 * explicit permission record exists for that user and tool.</p>
 */
@Entity
@Table(name = "permissions", indexes = {
        @Index(name = "idx_permissions_user_id", columnList = "user_id"),
        @Index(name = "idx_permissions_tool_id", columnList = "tool_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_permission_user_tool_level",
                columnNames = {"user_id", "tool_id", "access_level"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tool_id", nullable = false)
    private UUID toolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", insertable = false, updatable = false)
    private Tool tool;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 20)
    private AccessLevel accessLevel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
