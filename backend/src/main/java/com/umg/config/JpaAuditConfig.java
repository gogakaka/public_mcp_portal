package com.umg.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing so that {@code @CreatedDate} and {@code @LastModifiedDate}
 * annotations are automatically populated on entity persist and update operations.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
}
