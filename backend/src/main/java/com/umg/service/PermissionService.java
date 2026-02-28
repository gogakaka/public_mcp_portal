package com.umg.service;

import com.umg.domain.entity.Permission;
import com.umg.domain.entity.Tool;
import com.umg.domain.entity.User;
import com.umg.domain.enums.AccessLevel;
import com.umg.dto.PermissionRequest;
import com.umg.dto.PermissionResponse;
import com.umg.exception.DuplicateResourceException;
import com.umg.exception.ResourceNotFoundException;
import com.umg.repository.PermissionRepository;
import com.umg.repository.ToolRepository;
import com.umg.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service handling permission grant/revoke operations and access checks.
 */
@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final ToolRepository toolRepository;

    public PermissionService(PermissionRepository permissionRepository,
                             UserRepository userRepository,
                             ToolRepository toolRepository) {
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.toolRepository = toolRepository;
    }

    /**
     * Grants a permission to a user for a specific tool.
     *
     * @param request the permission grant request
     * @return the created permission response
     * @throws DuplicateResourceException if the permission already exists
     */
    @Transactional
    public PermissionResponse grantPermission(PermissionRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId().toString()));
        Tool tool = toolRepository.findById(request.getToolId())
                .orElseThrow(() -> new ResourceNotFoundException("Tool", request.getToolId().toString()));

        if (permissionRepository.existsByUserIdAndToolIdAndAccessLevel(
                request.getUserId(), request.getToolId(), request.getAccessLevel())) {
            throw new DuplicateResourceException("Permission",
                    String.format("user=%s, tool=%s, level=%s",
                            request.getUserId(), request.getToolId(), request.getAccessLevel()));
        }

        Permission permission = Permission.builder()
                .userId(request.getUserId())
                .toolId(request.getToolId())
                .accessLevel(request.getAccessLevel())
                .build();

        Permission saved = permissionRepository.save(permission);
        log.info("Permission granted: user={}, tool={}, level={}",
                user.getEmail(), tool.getName(), request.getAccessLevel());

        return toResponse(saved, user.getName(), tool.getName());
    }

    /**
     * Revokes a permission by its ID.
     *
     * @param permissionId the permission's UUID
     */
    @Transactional
    public void revokePermission(UUID permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", permissionId.toString()));

        permissionRepository.delete(permission);
        log.info("Permission revoked: id={}, user={}, tool={}",
                permissionId, permission.getUserId(), permission.getToolId());
    }

    /**
     * Revokes all permissions for a user on a specific tool.
     *
     * @param userId the user's UUID
     * @param toolId the tool's UUID
     */
    @Transactional
    public void revokePermissionByUserAndTool(UUID userId, UUID toolId) {
        permissionRepository.deleteByUserIdAndToolId(userId, toolId);
        log.info("All permissions revoked for user={} on tool={}", userId, toolId);
    }

    /**
     * Lists all permissions for a specific user.
     *
     * @param userId the user's UUID
     * @return list of permission responses
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissionsByUser(UUID userId) {
        return permissionRepository.findByUserId(userId).stream()
                .map(this::toResponseWithLookup)
                .toList();
    }

    /**
     * Lists all permissions for a specific tool.
     *
     * @param toolId the tool's UUID
     * @return list of permission responses
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissionsByTool(UUID toolId) {
        return permissionRepository.findByToolId(toolId).stream()
                .map(this::toResponseWithLookup)
                .toList();
    }

    /**
     * Checks whether a user has a specific access level on a tool, considering
     * both explicit permissions and the tool's public flag.
     *
     * @param userId      the user's UUID
     * @param toolId      the tool's UUID
     * @param accessLevel the required access level
     * @return {@code true} if the user has the required access
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(UUID userId, UUID toolId, AccessLevel accessLevel) {
        Tool tool = toolRepository.findById(toolId).orElse(null);
        if (tool == null) {
            return false;
        }

        // Public tools grant EXECUTE access to everyone
        if (Boolean.TRUE.equals(tool.getIsPublic()) && accessLevel == AccessLevel.EXECUTE) {
            return true;
        }

        // Owners have full access
        if (tool.getOwnerId().equals(userId)) {
            return true;
        }

        return permissionRepository.existsByUserIdAndToolIdAndAccessLevel(userId, toolId, accessLevel);
    }

    private PermissionResponse toResponse(Permission permission, String userName, String toolName) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .toolId(permission.getToolId())
                .toolName(toolName)
                .userId(permission.getUserId())
                .userName(userName)
                .accessLevel(permission.getAccessLevel())
                .createdAt(permission.getCreatedAt())
                .build();
    }

    private PermissionResponse toResponseWithLookup(Permission permission) {
        String userName = userRepository.findById(permission.getUserId())
                .map(User::getName).orElse("Unknown");
        String toolName = toolRepository.findById(permission.getToolId())
                .map(Tool::getName).orElse("Unknown");
        return toResponse(permission, userName, toolName);
    }
}
