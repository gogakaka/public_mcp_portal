package com.umg.domain.enums;

/**
 * Lifecycle status of a tool within the Maker-Checker approval workflow.
 *
 * <ul>
 *   <li>{@code PENDING} - Tool has been submitted and awaits admin review.</li>
 *   <li>{@code APPROVED} - Tool has been approved and is available for execution.</li>
 *   <li>{@code REJECTED} - Tool has been rejected by an admin.</li>
 * </ul>
 */
public enum ToolStatus {
    PENDING,
    APPROVED,
    REJECTED
}
