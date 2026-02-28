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
 * API 키별 속도 제한 서블릿 필터.
 *
 * <p>Bucket4j와 Redis를 사용하여 분산 환경에서의 속도 제한을 적용합니다.
 * 속도 제한은 다음 기준으로 결정됩니다:</p>
 * <ul>
 *   <li>API 키 인증 요청: 해당 키에 설정된 {@code rateLimitPerMin} 값</li>
 *   <li>기타 요청: 애플리케이션 기본 속도 제한 값</li>
 * </ul>
 *
 * <p>제한 초과 시 HTTP 429(Too Many Requests) 응답과 함께
 * {@code Retry-After} 헤더를 반환합니다.</p>
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

    /**
     * 각 요청에 대해 속도 제한을 검사합니다.
     *
     * <p>API 엔드포인트(/api/**)에 대해서만 속도 제한을 적용하며,
     * 헬스체크 등의 액추에이터 엔드포인트는 제외합니다.</p>
     *
     * @param request     HTTP 요청
     * @param response    HTTP 응답
     * @param filterChain 필터 체인
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        /* 비 API 엔드포인트는 속도 제한 건너뛰기 */
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        /* 액추에이터 엔드포인트는 속도 제한 건너뛰기 */
        if (path.startsWith("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey;
        int rateLimit;

        /* API 키 또는 IP 주소 기반으로 속도 제한 결정 */
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
            /* 남은 토큰 수를 응답 헤더에 포함 */
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            /* 속도 제한 초과: 429 응답 반환 */
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000 + 1;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.addHeader("X-Rate-Limit-Remaining", "0");

            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(429)
                    .error("Too Many Requests")
                    .message("속도 제한을 초과했습니다. " + retryAfterSeconds + "초 후에 다시 시도해주세요.")
                    .path(path)
                    .timestamp(Instant.now())
                    .build();

            objectMapper.writeValue(response.getOutputStream(), errorResponse);

            log.warn("속도 제한 초과 - 버킷: '{}', 경로: '{}'", bucketKey, path);
        }
    }

    /**
     * 클라이언트 IP 주소를 추출합니다.
     * X-Forwarded-For 헤더가 있으면 첫 번째 IP를 사용하고,
     * 없으면 원격 주소를 사용합니다.
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
