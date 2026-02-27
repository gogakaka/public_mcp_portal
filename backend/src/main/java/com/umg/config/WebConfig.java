package com.umg.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * General web configuration for the UMG backend.
 *
 * <p>Registers a servlet filter that assigns a unique trace ID to every
 * incoming request and places it in the SLF4J MDC so that all log lines
 * produced during request processing include the trace ID.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID_KEY = "traceId";

    /**
     * Filter that generates or propagates a trace ID for every HTTP request.
     * If the caller provides an {@code X-Trace-Id} header, it is reused;
     * otherwise a new UUID is generated.
     *
     * @return the trace ID filter
     */
    @Bean
    public OncePerRequestFilter traceIdFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String traceId = request.getHeader(TRACE_ID_HEADER);
                if (traceId == null || traceId.isBlank()) {
                    traceId = UUID.randomUUID().toString().replace("-", "");
                }
                MDC.put(MDC_TRACE_ID_KEY, traceId);
                response.setHeader(TRACE_ID_HEADER, traceId);
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    MDC.remove(MDC_TRACE_ID_KEY);
                }
            }
        };
    }
}
