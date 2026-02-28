package com.umg.repository;

import com.umg.domain.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ApiKey} entities.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    /**
     * Finds an API key by its SHA-256 hash.
     *
     * @param keyHash the SHA-256 hex digest of the raw API key
     * @return an optional containing the key record if found
     */
    Optional<ApiKey> findByKeyHash(String keyHash);

    /**
     * Retrieves all active API keys belonging to a user.
     *
     * @param userId the user's ID
     * @return list of active API keys
     */
    List<ApiKey> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Retrieves all API keys belonging to a user regardless of status.
     *
     * @param userId the user's ID
     * @return list of all API keys for the user
     */
    List<ApiKey> findByUserId(UUID userId);
}
