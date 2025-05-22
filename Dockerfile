# Build stage
FROM openjdk:21-jdk-slim AS builder
WORKDIR /app

# Copy gradle wrapper files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build application
RUN ./gradlew build --no-daemon -x test

# Runtime stage
FROM openjdk:21-jdk-slim
WORKDIR /app

# Create non-root user
RUN groupadd -r spring && useradd -r -g spring spring

# Copy JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Run with docker profile
ENTRYPOINT ["java", "-jar", "/app/app.jar"]