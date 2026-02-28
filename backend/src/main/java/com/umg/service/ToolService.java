package com.umg.service;

import com.umg.domain.entity.Tool;
import com.umg.domain.enums.ToolStatus;
import com.umg.dto.*;
import com.umg.exception.ForbiddenException;
import com.umg.exception.ResourceNotFoundException;
import com.umg.repository.ToolRepository;
import com.umg.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service handling tool CRUD operations and the Maker-Checker approval workflow.
 *
 * <p>When a user creates a tool it enters {@link ToolStatus#PENDING} status.
 * An admin must then approve or reject the tool before it becomes available
 * for execution.</p>
 */
@Service
public class ToolService {

    private static final Logger log = LoggerFactory.getLogger(ToolService.class);

    private final ToolRepository toolRepository;
    private final SecurityUtils securityUtils;

    public ToolService(ToolRepository toolRepository, SecurityUtils securityUtils) {
        this.toolRepository = toolRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Creates a new tool with PENDING status.
     *
     * @param request the tool creation request
     * @return the created tool response
     */
    @Transactional
    public ToolResponse createTool(ToolCreateRequest request) {
        UUID ownerId = securityUtils.requireCurrentUserId();

        Tool tool = Tool.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .toolType(request.getToolType())
                .connectionConfig(request.getConnectionConfig())
                .authType(request.getAuthType())
                .inputSchema(request.getInputSchema())
                .responseMappingRule(request.getResponseMappingRule())
                .status(ToolStatus.PENDING)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .isIdempotent(request.getIsIdempotent() != null ? request.getIsIdempotent() : false)
                .ownerId(ownerId)
                .build();

        Tool saved = toolRepository.save(tool);
        log.info("Tool '{}' created with PENDING status by user {}", saved.getName(), ownerId);
        return toResponse(saved);
    }

    /**
     * Retrieves a tool by its ID.
     *
     * @param toolId the tool's UUID
     * @return the tool response
     * @throws ResourceNotFoundException if the tool does not exist
     */
    @Transactional(readOnly = true)
    public ToolResponse getToolById(UUID toolId) {
        Tool tool = findToolOrThrow(toolId);
        return toResponse(tool);
    }

    /**
     * Retrieves the raw tool entity by ID (for internal use by adapters).
     *
     * @param toolId the tool's UUID
     * @return the Tool entity
     * @throws ResourceNotFoundException if the tool does not exist
     */
    @Transactional(readOnly = true)
    public Tool getToolEntityById(UUID toolId) {
        return findToolOrThrow(toolId);
    }

    /**
     * Updates a tool. Only the owner or an admin can update a tool.
     * Updating a tool resets its status to PENDING for re-approval.
     *
     * @param toolId  the tool's UUID
     * @param request the update request
     * @return the updated tool response
     */
    @Transactional
    public ToolResponse updateTool(UUID toolId, ToolUpdateRequest request) {
        Tool tool = findToolOrThrow(toolId);
        UUID currentUserId = securityUtils.requireCurrentUserId();

        if (!tool.getOwnerId().equals(currentUserId) && !securityUtils.isCurrentUserAdmin()) {
            throw new ForbiddenException("Only the tool owner or an admin can update this tool");
        }

        if (request.getName() != null) {
            tool.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            tool.setDescription(request.getDescription());
        }
        if (request.getConnectionConfig() != null) {
            tool.setConnectionConfig(request.getConnectionConfig());
        }
        if (request.getAuthType() != null) {
            tool.setAuthType(request.getAuthType());
        }
        if (request.getInputSchema() != null) {
            tool.setInputSchema(request.getInputSchema());
        }
        if (request.getResponseMappingRule() != null) {
            tool.setResponseMappingRule(request.getResponseMappingRule());
        }
        if (request.getIsPublic() != null) {
            tool.setIsPublic(request.getIsPublic());
        }
        if (request.getIsIdempotent() != null) {
            tool.setIsIdempotent(request.getIsIdempotent());
        }

        // Reset to PENDING for re-approval after update
        tool.setStatus(ToolStatus.PENDING);

        Tool updated = toolRepository.save(tool);
        log.info("Tool '{}' updated and reset to PENDING by user {}", updated.getName(), currentUserId);
        return toResponse(updated);
    }

    /**
     * Deletes a tool. Only the owner or an admin can delete a tool.
     *
     * @param toolId the tool's UUID
     */
    @Transactional
    public void deleteTool(UUID toolId) {
        Tool tool = findToolOrThrow(toolId);
        UUID currentUserId = securityUtils.requireCurrentUserId();

        if (!tool.getOwnerId().equals(currentUserId) && !securityUtils.isCurrentUserAdmin()) {
            throw new ForbiddenException("Only the tool owner or an admin can delete this tool");
        }

        toolRepository.delete(tool);
        log.info("Tool '{}' deleted by user {}", tool.getName(), currentUserId);
    }

    /**
     * Approves a pending tool (admin only).
     *
     * @param toolId the tool's UUID
     * @return the updated tool response
     */
    @Transactional
    public ToolResponse approveTool(UUID toolId) {
        Tool tool = findToolOrThrow(toolId);

        if (tool.getStatus() != ToolStatus.PENDING) {
            throw new IllegalStateException("Only PENDING tools can be approved. Current status: " + tool.getStatus());
        }

        tool.setStatus(ToolStatus.APPROVED);
        Tool updated = toolRepository.save(tool);
        log.info("Tool '{}' approved by admin {}", updated.getName(), securityUtils.requireCurrentUserId());
        return toResponse(updated);
    }

    /**
     * Rejects a pending tool (admin only).
     *
     * @param toolId the tool's UUID
     * @return the updated tool response
     */
    @Transactional
    public ToolResponse rejectTool(UUID toolId) {
        Tool tool = findToolOrThrow(toolId);

        if (tool.getStatus() != ToolStatus.PENDING) {
            throw new IllegalStateException("Only PENDING tools can be rejected. Current status: " + tool.getStatus());
        }

        tool.setStatus(ToolStatus.REJECTED);
        Tool updated = toolRepository.save(tool);
        log.info("Tool '{}' rejected by admin {}", updated.getName(), securityUtils.requireCurrentUserId());
        return toResponse(updated);
    }

    /**
     * Lists tools by status with pagination.
     *
     * @param status   the status filter
     * @param pageable pagination information
     * @return a page of tool responses
     */
    @Transactional(readOnly = true)
    public PageResponse<ToolResponse> listByStatus(ToolStatus status, Pageable pageable) {
        Page<Tool> page = toolRepository.findByStatus(status, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    /**
     * Lists tools accessible by the current user with pagination.
     *
     * @param pageable pagination information
     * @return a page of accessible tool responses
     */
    @Transactional(readOnly = true)
    public PageResponse<ToolResponse> listAccessibleTools(Pageable pageable) {
        UUID userId = securityUtils.requireCurrentUserId();
        Page<Tool> page;

        if (securityUtils.isCurrentUserAdmin()) {
            page = toolRepository.findAll(pageable);
        } else {
            page = toolRepository.findAccessibleByUserId(userId, pageable);
        }

        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    /**
     * Lists all approved tools accessible by a user (no pagination, for MCP tools/list).
     *
     * @param userId the user's ID
     * @return list of accessible tools
     */
    @Transactional(readOnly = true)
    public List<Tool> listAllAccessibleTools(UUID userId) {
        return toolRepository.findAllAccessibleByUserId(userId);
    }

    private Tool findToolOrThrow(UUID toolId) {
        return toolRepository.findById(toolId)
                .orElseThrow(() -> new ResourceNotFoundException("Tool", toolId.toString()));
    }

    private ToolResponse toResponse(Tool tool) {
        return ToolResponse.builder()
                .id(tool.getId())
                .name(tool.getName())
                .description(tool.getDescription())
                .toolType(tool.getToolType())
                .authType(tool.getAuthType())
                .inputSchema(tool.getInputSchema())
                .responseMappingRule(tool.getResponseMappingRule())
                .status(tool.getStatus())
                .isPublic(tool.getIsPublic())
                .isIdempotent(tool.getIsIdempotent())
                .ownerId(tool.getOwnerId())
                .cubeDatasourceId(tool.getCubeDatasourceId())
                .awsServerId(tool.getAwsServerId())
                .createdAt(tool.getCreatedAt())
                .updatedAt(tool.getUpdatedAt())
                .build();
    }
}
