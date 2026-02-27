package com.umg.domain.enums;

/**
 * Outcome status recorded for a tool execution in the audit log.
 *
 * <ul>
 *   <li>{@code SUCCESS} - The tool executed without errors.</li>
 *   <li>{@code FAIL} - The tool execution failed or threw an exception.</li>
 * </ul>
 */
public enum AuditStatus {
    SUCCESS,
    FAIL
}
