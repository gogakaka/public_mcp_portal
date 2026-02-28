package com.umg.domain.enums;

/**
 * Classification of tool backends that the UMG gateway can proxy requests to.
 *
 * <ul>
 *   <li>{@code N8N} - n8n workflow automation webhooks.</li>
 *   <li>{@code CUBE_JS} - Cube.js semantic-layer analytics queries.</li>
 *   <li>{@code AWS_REMOTE} - Remote MCP servers hosted on AWS with SigV4 authentication.</li>
 * </ul>
 */
public enum ToolType {
    N8N,
    CUBE_JS,
    AWS_REMOTE
}
