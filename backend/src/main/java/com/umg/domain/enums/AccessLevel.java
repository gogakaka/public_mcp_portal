package com.umg.domain.enums;

/**
 * Permission levels that can be granted to a user for a specific tool.
 *
 * <ul>
 *   <li>{@code EXECUTE} - User may invoke the tool but cannot modify its configuration.</li>
 *   <li>{@code EDIT} - User may both invoke the tool and modify its configuration.</li>
 * </ul>
 */
public enum AccessLevel {
    EXECUTE,
    EDIT
}
