package com.umg.domain.enums;

/**
 * Authentication mechanisms supported when the gateway connects to a backend tool.
 *
 * <ul>
 *   <li>{@code BEARER} - Bearer token authentication via Authorization header.</li>
 *   <li>{@code AWS_SIGV4} - AWS Signature Version 4 request signing.</li>
 *   <li>{@code NONE} - No authentication required (e.g. internal network tools).</li>
 * </ul>
 */
public enum AuthType {
    BEARER,
    AWS_SIGV4,
    NONE
}
