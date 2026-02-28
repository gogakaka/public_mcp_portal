package com.umg.service;

import com.umg.domain.entity.ApiKey;
import com.umg.dto.ApiKeyCreateRequest;
import com.umg.dto.ApiKeyResponse;
import com.umg.exception.ResourceNotFoundException;
import com.umg.repository.ApiKeyRepository;
import com.umg.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Service handling API key generation, revocation, and listing.
 *
 * <p>Raw API keys are generated as cryptographically secure random strings.
 * Only the SHA-256 hash is stored in the database. The raw key is returned
 * exactly once at creation time and cannot be recovered afterward.</p>
 */
@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final int RAW_KEY_BYTE_LENGTH = 32;
    private static final String KEY_PREFIX = "umg_";

    private final ApiKeyRepository apiKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Generates a new API key for the specified user.
     *
     * @param userId  the user's UUID
     * @param request the key creation request
     * @return the key response containing the raw key (returned only once)
     */
    @Transactional
    public ApiKeyResponse createApiKey(UUID userId, ApiKeyCreateRequest request) {
        String rawKey = generateRawKey();
        String keyHash = HashUtil.sha256(rawKey);

        ApiKey apiKey = ApiKey.builder()
                .userId(userId)
                .keyHash(keyHash)
                .name(request.getName().trim())
                .rateLimitPerMin(request.getRateLimitPerMin() != null ? request.getRateLimitPerMin() : 60)
                .expiresAt(request.getExpiresAt())
                .isActive(true)
                .build();

        ApiKey saved = apiKeyRepository.save(apiKey);
        log.info("API key '{}' created for user {}", saved.getName(), userId);

        return ApiKeyResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .rawKey(rawKey)
                .rateLimitPerMin(saved.getRateLimitPerMin())
                .expiresAt(saved.getExpiresAt())
                .isActive(saved.getIsActive())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /**
     * Lists all API keys for a user. Raw keys are not included.
     *
     * @param userId the user's UUID
     * @return list of key responses (without raw keys)
     */
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listApiKeys(UUID userId) {
        return apiKeyRepository.findByUserId(userId).stream()
                .map(this::toResponseWithoutRawKey)
                .toList();
    }

    /**
     * Revokes (deactivates) an API key.
     *
     * @param userId the user's UUID (for ownership verification)
     * @param keyId  the API key's UUID
     */
    @Transactional
    public void revokeApiKey(UUID userId, UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", keyId.toString()));

        if (!apiKey.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("ApiKey", keyId.toString());
        }

        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
        log.info("API key '{}' revoked for user {}", apiKey.getName(), userId);
    }

    /**
     * Generates a cryptographically secure random API key string.
     *
     * @return the raw key string with the "umg_" prefix
     */
    private String generateRawKey() {
        byte[] bytes = new byte[RAW_KEY_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ApiKeyResponse toResponseWithoutRawKey(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .rawKey(null)
                .rateLimitPerMin(apiKey.getRateLimitPerMin())
                .expiresAt(apiKey.getExpiresAt())
                .isActive(apiKey.getIsActive())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }
}
