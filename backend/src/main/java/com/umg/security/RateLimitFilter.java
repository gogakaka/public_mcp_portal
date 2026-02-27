package com.umg.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.config.RateLimitConfig;
import com.umg.domain.entity.ApiKey;
import com.umg.dto.ErrorResponse;
import com.umg.repository.ApiKeyRepository;
import com.umg.util.HashUtil;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Servlet filter that enforces per-client rate limiting using Bucket4j
 * with Redis-backed distributed buckets.
 *
 * <p>Rate limits are determined by:</p>
 * <ul>
 *   <li>For API key authenticated requests: the key's configured {@code rateLimitPerMin}</li>
 *   <li>For all other requests: the application's default rate limit</li>
 * </ul>
 *
 * <p>When the rate limit is exceeded, the filter returns HTTP 429 with
 * a {@code Retry-After} header indicating when the client may retry.</p>
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String RATE_LIMIT_BUCKET_PREFIX = "rate-limit:";

    private final ProxyManager<String> proxyManager;
    private final ApiKeyRepository apiKeyRepository;
    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ProxyManager<String> proxyManager,
                           ApiKeyRepository apiKeyRepository,
                           RateLimitConfig rateLimitConfig,
                           ObjectMapper objectMapper) {
        this.proxyManager = proxyManager;
        this.apiKeyRepository = apiKeyRepository;
        this.rateLimitConfig = rateLimitConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip rate limiting for non-API endpoints
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for health checks
        if (path.startsWith("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey;
        int rateLimit;

        // Determine rate limit based on API key or IP
        String rawApiKey = request.getHeader(API_KEY_HEADER);
        if (rawApiKey != null && !rawApiKey.isBlank()) {
            String keyHash = HashUtil.sha256(rawApiKey);
            ApiKey apiKey = apiKeyRepository.findByKeyHash(keyHash).orElse(null);
            if (apiKey != null) {
                bucketKey = RATE_LIMIT_BUCKET_PREFIX + "key:" + keyHash;
                rateLimit = apiKey.getRateLimitPerMin();
            } else {
                bucketKey = RATE_LIMIT_BUCKET_PREFIX + "ip:" + getClientIp(request);
                rateLimit = rateLimitConfig.getDefaultRateLimit();
            }
        } else {
            bucketKey = RATE_LIMIT_BUCKET_PREFIX + "ip:" + getClientIp(request);
            rateLimit = rateLimitConfig.getDefaultRateLimit();
        }

        Supplier<BucketConfiguration> configSupplier =
                () -> RateLimitConfig.createBucketConfiguration(rateLimit);

        Bucket bucket = proxyManager.builder()
                .build(bucketKey, configSupplier);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000 + 1;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.addHeader("X-Rate-Limit-Remaining", "0");

            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(429)
                    .error("Too Many Requests")
                    .message("Rate limit exceeded. Please retry after " + retryAfterSeconds + " seconds.")
                    .path(path)
                    .timestamp(Instant.now())
                    .build();

            objectMapper.writeValue(response.getOutputStream(), errorResponse);

            log.warn("Rate limit exceeded for bucket '{}' on path '{}'", bucketKey, path);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
