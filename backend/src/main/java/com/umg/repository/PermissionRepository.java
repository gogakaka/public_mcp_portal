package com.umg.repository;

import com.umg.domain.entity.Permission;
import com.umg.domain.enums.AccessLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Permission} entities.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    /**
     * Finds all permissions granted to a specific user.
     *
     * @param userId the user's ID
     * @return list of permissions
     */
    List<Permission> findByUserId(UUID userId);

    /**
     * Finds all permissions granted for a specific tool.
     *
     * @param toolId the tool's ID
     * @return list of permissions
     */
    List<Permission> findByToolId(UUID toolId);

    /**
     * Finds the permission record linking a user to a tool.
     *
     * @param userId the user's ID
     * @param toolId the tool's ID
     * @return an optional containing the permission if found
     */
    Optional<Permission> findByUserIdAndToolId(UUID userId, UUID toolId);

    /**
     * Checks whether a user has a specific access level on a tool.
     *
     * @param userId      the user's ID
     * @param toolId      the tool's ID
     * @param accessLevel the required access level
     * @return {@code true} if the permission exists
     */
    boolean existsByUserIdAndToolIdAndAccessLevel(UUID userId, UUID toolId, AccessLevel accessLevel);

    /**
     * Deletes all permissions for a specific tool (used when a tool is removed).
     *
     * @param toolId the tool's ID
     */
    void deleteByToolId(UUID toolId);

    /**
     * Deletes the specific permission record for a user on a tool.
     *
     * @param userId the user's ID
     * @param toolId the tool's ID
     */
    void deleteByUserIdAndToolId(UUID userId, UUID toolId);
}
