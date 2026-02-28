package com.umg.repository;

import com.umg.domain.entity.Tool;
import com.umg.domain.enums.ToolStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Tool} entities.
 */
@Repository
public interface ToolRepository extends JpaRepository<Tool, UUID> {

    /**
     * Finds tools filtered by their approval status.
     *
     * @param status   the status to filter by
     * @param pageable pagination information
     * @return a page of tools matching the status
     */
    Page<Tool> findByStatus(ToolStatus status, Pageable pageable);

    /**
     * Finds all publicly available tools.
     *
     * @param pageable pagination information
     * @return a page of public tools
     */
    Page<Tool> findByIsPublicTrue(Pageable pageable);

    /**
     * Finds tools owned by a specific user with any of the given statuses.
     *
     * @param ownerId  the owner's user ID
     * @param statuses collection of statuses to include
     * @param pageable pagination information
     * @return a page of matching tools
     */
    Page<Tool> findByOwnerIdAndStatusIn(UUID ownerId, Collection<ToolStatus> statuses, Pageable pageable);

    /**
     * Finds all approved tools accessible by a user. A tool is accessible if it
     * is public or if an explicit permission exists for the user.
     *
     * @param userId   the user's ID
     * @param pageable pagination information
     * @return a page of accessible tools
     */
    @Query("""
            SELECT t FROM Tool t
            WHERE t.status = 'APPROVED'
              AND (t.isPublic = true
                   OR t.ownerId = :userId
                   OR EXISTS (SELECT 1 FROM Permission p WHERE p.toolId = t.id AND p.userId = :userId))
            """)
    Page<Tool> findAccessibleByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Finds all approved tools accessible by a user as a list (no pagination).
     *
     * @param userId the user's ID
     * @return list of accessible tools
     */
    @Query("""
            SELECT t FROM Tool t
            WHERE t.status = 'APPROVED'
              AND (t.isPublic = true
                   OR t.ownerId = :userId
                   OR EXISTS (SELECT 1 FROM Permission p WHERE p.toolId = t.id AND p.userId = :userId))
            """)
    List<Tool> findAllAccessibleByUserId(@Param("userId") UUID userId);

    /**
     * Counts tools grouped by status (for dashboard statistics).
     *
     * @param status the status to count
     * @return the number of tools with the given status
     */
    long countByStatus(ToolStatus status);

    /**
     * Finds a tool by its name and associated AWS MCP server ID.
     *
     * @param name        the tool name
     * @param awsServerId the AWS MCP server ID
     * @return an optional containing the tool if found
     */
    Optional<Tool> findByNameAndAwsServerId(String name, UUID awsServerId);
}
