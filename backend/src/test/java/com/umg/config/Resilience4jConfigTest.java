package com.umg.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Resilience4jConfig 단위 테스트")
class Resilience4jConfigTest {

    private final Resilience4jConfig config = new Resilience4jConfig();

    @Test
    @DisplayName("서킷 브레이커 레지스트리가 n8n, cubeJs, awsMcp 설정을 포함한다")
    void shouldContainAllConfigurations() {
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();

        CircuitBreaker n8n = registry.circuitBreaker("n8n", "n8n");
        CircuitBreaker cubeJs = registry.circuitBreaker("cubeJs", "cubeJs");
        CircuitBreaker awsMcp = registry.circuitBreaker("awsMcp", "awsMcp");

        assertThat(n8n).isNotNull();
        assertThat(cubeJs).isNotNull();
        assertThat(awsMcp).isNotNull();

        // cubeJs has longer slow call threshold
        assertThat(cubeJs.getCircuitBreakerConfig().getSlowCallDurationThreshold().getSeconds())
                .isEqualTo(30);
        assertThat(n8n.getCircuitBreakerConfig().getSlowCallDurationThreshold().getSeconds())
                .isEqualTo(10);
    }

    @Test
    @DisplayName("기본 서킷 브레이커 설정이 올바르다")
    void shouldHaveCorrectDefaults() {
        CircuitBreakerRegistry registry = config.circuitBreakerRegistry();
        CircuitBreaker cb = registry.circuitBreaker("n8n", "n8n");

        assertThat(cb.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50);
        assertThat(cb.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(10);
        assertThat(cb.getCircuitBreakerConfig().getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(cb.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
    }
}
