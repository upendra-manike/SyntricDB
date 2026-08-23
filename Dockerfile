# ==============================================================================
# SyntricDB Production Multi-Stage Dockerfile
# Base Image: Eclipse Temurin Java 17 LTS on Alpine Linux
# ==============================================================================

# --- Stage 1: Maven Build & Packaging Stage ---
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build shaded production JAR
COPY src ./src
COPY deploy ./deploy
RUN mvn clean package -DskipTests -B

# --- Stage 2: Production Runtime Stage ---
FROM eclipse-temurin:17-jre-alpine

# Set labels for GitHub Container Registry
LABEL org.opencontainers.image.title="SyntricDB" \
      org.opencontainers.image.description="Next-Generation AI-Native Unified Database Engine" \
      org.opencontainers.image.url="https://github.com/upendra-manike/SyntricDB" \
      org.opencontainers.image.licenses="Apache-2.0"

# Install curl/wget for healthchecks and runtime diagnostic utilities
RUN apk add --no-cache curl wget bash

# Create non-root system user & group
RUN addgroup -S syntricdb && adduser -S syntricdb -G syntricdb

# Setup application directories and persistent storage path
WORKDIR /opt/syntricdb
RUN mkdir -p /var/lib/syntricdb/data /var/lib/syntricdb/wal /var/lib/syntricdb/snapshots /etc/syntricdb && \
    chown -R syntricdb:syntricdb /opt/syntricdb /var/lib/syntricdb /etc/syntricdb

# Copy built JAR and production configuration from builder stage
COPY --from=builder --chown=syntricdb:syntricdb /app/target/syntricdb-engine-*.jar /opt/syntricdb/syntricdb-engine.jar
COPY --from=builder --chown=syntricdb:syntricdb /app/deploy/syntricdb.conf /etc/syntricdb/syntricdb.conf

# Environment Defaults for Container Runtime
ENV SYNTRICDB_BIND_ADDRESS="0.0.0.0" \
    SYNTRICDB_PORT="8080" \
    SYNTRICDB_DATA_DIR="/var/lib/syntricdb/data"

# Expose HTTP REST, HNSW Vector & Web Dashboard Port
EXPOSE 8080

# Declare persistent storage volumes
VOLUME ["/var/lib/syntricdb"]

# Switch to non-root execution context
USER syntricdb

# Container Healthcheck Probe
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/sql || exit 1

# Production Execution Entrypoint
ENTRYPOINT ["java", "-Xms256m", "-Xmx2g", "-XX:+UseG1GC", "-jar", "/opt/syntricdb/syntricdb-engine.jar"]
