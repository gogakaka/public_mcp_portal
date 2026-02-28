package com.umg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the Universal MCP Gateway (UMG) backend application.
 *
 * <p>This Spring Boot application provides a unified gateway for managing
 * and executing Model Context Protocol (MCP) tools across various backends
 * including n8n workflows, Cube.js analytics, and remote AWS MCP servers.</p>
 *
 * <p>Virtual threads are enabled via configuration to maximise throughput
 * for IO-bound tool executions without blocking platform threads.</p>
 */
@SpringBootApplication
@EnableAsync
public class UmgApplication {

    public static void main(String[] args) {
        SpringApplication.run(UmgApplication.class, args);
    }
}
