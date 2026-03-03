package com.umg.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Spring Cache 설정 (Redis 기반).
 *
 * <p>자주 조회되는 도구 정의, 사용자 정보, 권한 데이터에 대한
 * 캐싱을 활성화하여 DB 부하를 줄이고 응답 속도를 개선합니다.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_TOOL_BY_ID = "toolById";
    public static final String CACHE_USER_BY_ID = "userById";
    public static final String CACHE_ACCESSIBLE_TOOLS = "accessibleTools";
    public static final String CACHE_PERMISSIONS_BY_USER = "permissionsByUser";
    public static final String CACHE_CUBE_SCHEMA_META = "cubeSchemasMeta";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                CACHE_TOOL_BY_ID, defaultConfig.entryTtl(Duration.ofMinutes(5)),
                CACHE_USER_BY_ID, defaultConfig.entryTtl(Duration.ofMinutes(5)),
                CACHE_ACCESSIBLE_TOOLS, defaultConfig.entryTtl(Duration.ofMinutes(2)),
                CACHE_PERMISSIONS_BY_USER, defaultConfig.entryTtl(Duration.ofMinutes(2)),
                CACHE_CUBE_SCHEMA_META, defaultConfig.entryTtl(Duration.ofMinutes(10))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}
