package com.umg.security;

import com.umg.domain.entity.ApiKey;
import com.umg.domain.entity.User;
import com.umg.repository.ApiKeyRepository;
import com.umg.repository.UserRepository;
import com.umg.util.HashUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Servlet filter that authenticates requests bearing an {@code X-API-Key} header.
 *
 * <p>The raw API key from the header is hashed and looked up in the database.
 * If a matching, active, non-expired key is found, the request is authenticated
 * as the key's owner with their role-based authorities.</p>
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    public ApiKeyAuthFilter(ApiKeyRepository apiKeyRepository, UserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String rawApiKey = request.getHeader(API_KEY_HEADER);

        if (rawApiKey != null && !rawApiKey.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String keyHash = HashUtil.sha256(rawApiKey);
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHash(keyHash);

            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();

                if (Boolean.TRUE.equals(apiKey.getIsActive()) && !isExpired(apiKey)) {
                    Optional<User> userOpt = userRepository.findById(apiKey.getUserId());

                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user.getId().toString(),
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("Authenticated via API key '{}' for user '{}'", apiKey.getName(), user.getEmail());
                    }
                } else {
                    log.warn("API key is inactive or expired: {}", apiKey.getId());
                }
            } else {
                log.debug("No matching API key found for the provided hash");
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExpired(ApiKey apiKey) {
        return apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(Instant.now());
    }
}
