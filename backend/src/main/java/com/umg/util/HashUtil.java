package com.umg.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for cryptographic hashing operations.
 *
 * <p>Used primarily for hashing API keys before storage. The SHA-256
 * hash of a raw API key is stored in the database, allowing key
 * verification without exposing the raw key.</p>
 */
public final class HashUtil {

    private HashUtil() {
    }

    /**
     * Computes the SHA-256 hash of the input string.
     *
     * @param input the string to hash
     * @return the lowercase hex-encoded SHA-256 digest (64 characters)
     * @throws RuntimeException if SHA-256 is not available (should never happen)
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
