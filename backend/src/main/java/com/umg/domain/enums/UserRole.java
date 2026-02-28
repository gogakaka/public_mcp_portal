package com.umg.domain.enums;

/**
 * Roles available for users within the UMG system.
 *
 * <ul>
 *   <li>{@code ADMIN} - Full access including tool approval, user management, and audit review.</li>
 *   <li>{@code USER} - Standard access to execute approved tools and manage own resources.</li>
 * </ul>
 */
public enum UserRole {
    ADMIN,
    USER
}
