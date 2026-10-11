---
name: backend-testing
description: >-
  Procedures, best practices, and fast commands for testing the QueZap Java 25 / Spring Boot backend
  with NullAway, Spotless, Checkstyle, MockMvc, and JaCoCo.
---

# Backend Testing Guide (QueZap)

This skill provides guidelines and commands to execute fast, reliable, and high-performance tests on the QueZap backend.

## ⚡ Fast Feedback Loop (Targeted Testing)

Do not run the entire build when testing local modifications. Target the specific module and test class:

```bash
# Test a single class in quezap-api (e.g. AuthControllerTest)
./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.routes.AuthControllerTest"

# Test a single method
./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.routes.AuthControllerTest.shouldLoginSuccessfully"

# Test all unit tests in pure domain (instant execution, 0 Spring context)
./back/gradlew -p back :modules:domain:test

# Test application use cases
./back/gradlew -p back :modules:application:test

# Test infrastructure adapters (JPA, S3, JWT)
./back/gradlew -p back :modules:infrastructure:test
```

---

## 🔒 Strict Null Safety & Quality Gates

Always verify NullAway and formatting before concluding a backend change:

```bash
# 1. NullAway verification (compilation with -Werror)
./back/gradlew -p back clean test -Pnullaway=true

# 2. Code formatting (Spotless / Google Java Format)
./back/gradlew -p back spotlessApply
./back/gradlew -p back spotlessCheck

# 3. Checkstyle verification
./back/gradlew -p back checkstyleMain checkstyleTest
```

---

## 🔄 OpenAPI Synchronization Test

When REST endpoints or DTOs change in `quezap-api`, regenerate the OpenAPI schema for the frontend:

```bash
# Export OpenAPI specification directly to front/openapi.json
./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.GenerateOpenApiJsonTest"
```

Then synchronize the frontend TypeScript client:
```bash
cd front && bun run codegen
```

---

## 🏗️ Testing Patterns per Hexagonal Layer

1. **`modules:domain`**:
   - Zero Spring context, zero mocks.
   - Instantiate aggregates and value objects directly.
   - Assert domain invariants and `Domain.checkDomain(...)` throws `DomainRuleException`.

2. **`modules:application`**:
   - Use Mockito for repository ports and `UnitOfWorkEvents`.
   - Test handlers in isolation (`Handler.handle(input)`).
   - Verify published events on `UnitOfWorkEvents`.

3. **`modules:infrastructure`**:
   - Test JPA adapters using H2 or test PostgreSQL container.
   - Verify mapping between domain aggregates and JPA entities (`UserJpaEntity`, `SessionJpaEntity`, etc.).
   - Verify hashing algorithms (Argon2 for passwords, SHA-256 for credentials).

4. **`quezap-api`**:
   - Use `@SpringBootTest` + `@AutoConfigureMockMvc` for slice/controller testing.
   - Mock application handlers or underlying services when testing HTTP serialization, status codes, and exception translation (`GlobalExceptionHandler`).
