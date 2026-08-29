# Multi-stage Dockerfile for QueZap Backend (Spring Boot 3.5+ on Java 25)
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace

# Copy gradle wrapper and dependency descriptors
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY gradle/libs.versions.toml gradle/
COPY checkstyle checkstyle

# Copy all source modules
COPY modules modules
COPY quezap-api quezap-api
COPY quezap-cli quezap-cli

# Build the executable Spring Boot jar
RUN chmod +x gradlew && ./gradlew :quezap-api:bootJar -x test --no-daemon

# Runtime Stage
FROM eclipse-temurin:25-jdk

WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=builder /workspace/quezap-api/build/libs/quezap-api.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
