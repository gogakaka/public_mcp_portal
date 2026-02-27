# =============================================================================
# Multi-stage Dockerfile for UMG Backend
# Uses Eclipse Temurin JDK 21 for building and JRE 21 for runtime
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Build
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build configuration first (for dependency caching)
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY gradlew ./
RUN chmod +x gradlew

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# ---------------------------------------------------------------------------
# Stage 2: Runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for health check
RUN apk add --no-cache curl

# Security: run as non-root user
RUN addgroup -S umg && adduser -S umg -G umg

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Set ownership
RUN chown -R umg:umg /app

# Expose the application port
EXPOSE 8080

# Switch to non-root user
USER umg

# JVM options optimised for containers with Virtual Threads
ENV JAVA_OPTS="-XX:+UseZGC \
  -XX:+ZGenerational \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseContainerSupport \
  -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
