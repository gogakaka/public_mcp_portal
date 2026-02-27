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
 * Bucket4j rate limiting configuration backed by Redis.
 *
 * <p>Uses Lettuce for Redis communication and provides a distributed
 * {@link ProxyManager} that coordinates rate-limit buckets across
 * multiple application instances.</p>
 */
@Configuration
public class RateLimitConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${app.rate-limit.default-per-minute:60}")
    private int defaultRateLimit;

    /**
     * Creates the Bucket4j proxy manager backed by Redis Lettuce.
     *
     * @return a ProxyManager for distributed rate-limit buckets
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
     * Creates the default bucket configuration for standard rate limiting.
     *
     * @param requestsPerMinute the number of allowed requests per minute
     * @return a BucketConfiguration
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
     * Returns the default rate limit (requests per minute).
     *
     * @return the default rate limit
     */
    public int getDefaultRateLimit() {
        return defaultRateLimit;
    }
}
