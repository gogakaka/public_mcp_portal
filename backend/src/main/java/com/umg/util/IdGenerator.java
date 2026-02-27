package com.umg.util;

import java.util.UUID;

/**
 * Utility class for UUID generation.
 *
 * <p>Provides a centralised place for ID generation so that the strategy
 * can be changed uniformly (e.g. to UUIDv7 for time-ordered IDs).</p>
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    /**
     * Generates a new random UUID (version 4).
     *
     * @return a new UUID
     */
    public static UUID newId() {
        return UUID.randomUUID();
    }

    /**
     * Generates a new UUID string without hyphens.
     *
     * @return a 32-character hex string
     */
    public static String newIdString() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
