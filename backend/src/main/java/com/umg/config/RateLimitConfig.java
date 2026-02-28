package com.umg.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Bucket4j + Redis 기반 속도 제한 설정.
 *
 * <p>Lettuce 클라이언트를 사용하여 Redis와 통신하며, 분산 환경에서
 * 여러 애플리케이션 인스턴스 간에 속도 제한 버킷을 공유합니다.
 * {@link ProxyManager}를 통해 API 키별 또는 IP별 버킷을 관리합니다.</p>
 */
@Configuration
public class RateLimitConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /** 기본 분당 요청 허용 횟수 */
    @Value("${app.rate-limit.default-per-minute:60}")
    private int defaultRateLimit;

    /**
     * Redis Lettuce 기반의 Bucket4j 프록시 매니저 빈을 생성합니다.
     * 분산 환경에서 속도 제한 버킷을 공유하는 데 사용됩니다.
     *
     * @return 분산 속도 제한 버킷용 ProxyManager
     */
    @Bean
    public ProxyManager<String> proxyManager() {
        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort);

        if (redisPassword != null && !redisPassword.isBlank()) {
            uriBuilder.withPassword(redisPassword.toCharArray());
        }

        RedisClient redisClient = RedisClient.create(uriBuilder.build());
        StatefulRedisConnection<String, byte[]> connection =
                redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        return LettuceBasedProxyManager.builderFor(connection)
                .build();
    }

    /**
     * 지정된 분당 요청 횟수로 버킷 설정을 생성합니다.
     *
     * <p>Greedy 리필 전략을 사용하여 1분 단위로 토큰을 보충합니다.</p>
     *
     * @param requestsPerMinute 분당 허용 요청 수
     * @return 속도 제한 BucketConfiguration
     */
    public static BucketConfiguration createBucketConfiguration(int requestsPerMinute) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requestsPerMinute)
                        .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    /**
     * 기본 속도 제한 값(분당 요청 수)을 반환합니다.
     *
     * @return 기본 분당 요청 허용 횟수
     */
    public int getDefaultRateLimit() {
        return defaultRateLimit;
    }
}
