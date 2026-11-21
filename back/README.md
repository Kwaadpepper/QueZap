# QueZap — Backend

Spring Boot 3.5.16 (Java 25) backend built with Domain-Driven Design (DDD) and Hexagonal Architecture. Exposes:
- a stateless REST API (`quezap-api`) with Server-Sent Events (SSE)
- an interactive Spring Shell admin CLI (`quezap-cli`)
- GraalVM 25 native executables

[Root documentation](../README.md)

---

## 🔧 Prerequisites

- **Java 25 JDK** (`>= 25.0.5.1` recommended for security fixes) — the Gradle toolchain targets Java 25
- **GraalVM 25** (optional, required for `nativeCompile`)
- PostgreSQL 17 and RustFS (start both locally with `docker compose -f ../docker-compose.dev.yml up -d postgres rustfs rustfs-init`)

---

## 🧭 Multi-Module Architecture

- `modules:domain` — pure business model (aggregates, value objects, invariants, domain events, repository ports; zero Spring dependencies)
- `modules:application` — application ports/use cases (`@Usecase`), transactional execution, domain event publication
- `modules:infrastructure` — technical adapters (JPA/Hibernate, InMemory, S3/RustFS, Argon2/JWT security, event listeners)
- `modules:shared` — cross-cutting DDD primitives (typed IDs, UUIDv7, pagination)
- Applications:
  - `quezap-api` — Spring Boot REST & SSE adapter
  - `quezap-cli` — Spring Shell admin CLI adapter

---

## 🚀 Quick Start

Start the REST API:
```bash
./gradlew :quezap-api:bootRun
```

Start the interactive CLI:
```bash
./gradlew :quezap-cli:bootRun --console=plain
```

Run the CLI inside Docker Compose:
```bash
docker compose -f ../docker-compose.dev.yml --profile cli run --rm cli
```

Create a user inside the interactive shell (password is prompted securely on TTY):
```text
users add --name <name> --email <email>
```

Available Spring profiles: `default`, `cli`, `test`, `prod` (see `quezap-api/src/main/resources/application*.yml`):
```bash
SPRING_PROFILES_ACTIVE=prod ./gradlew :quezap-api:bootRun
```

---

## 🧪 Testing & Code Quality

Run Spotless formatting, Checkstyle, NullAway (`-Werror`), and all unit/integration tests:
```bash
./gradlew spotlessApply checkstyleMain checkstyleTest test -Pnullaway=true
```

Run a single test class:
```bash
./gradlew :quezap-api:test --tests "com.quezap.api.v1.routes.AuthControllerTest"
```

Generate JaCoCo coverage reports (e.g. `quezap-api/build/reports/jacoco/test/html/index.html`):
```bash
./gradlew test jacocoTestReport
```

Regenerate `front/openapi.json` after changing REST controllers or DTOs:
```bash
./gradlew :quezap-api:test --tests "com.quezap.api.v1.GenerateOpenApiJsonTest"
```

---

## 📦 Build & GraalVM Native Image

Build executable JARs:
```bash
./gradlew clean build -Pnullaway=true
./gradlew :quezap-api:bootJar
./gradlew :quezap-cli:bootJar
```

Verify Spring AOT processing without running a full native build:
```bash
./gradlew :quezap-api:processAot :quezap-api:compileAotTestJava :quezap-cli:processAot
```

Compile GraalVM native executables:
```bash
./gradlew :quezap-api:nativeCompile
./gradlew :quezap-cli:nativeCompile
```

Run the compiled native binaries:
```bash
./quezap-api/build/native/nativeCompile/quezap-api
./quezap-cli/build/native/nativeCompile/quezap-cli
```

### AOT Runtime Hints & GraalVM Agent

- **Programmatic hints**:
  - `com.quezap.application.aot.QuezapRuntimeHints` — reflection hints for `ConstraintValidator`, `Converter`, `JsonDeserializer`, DTO records, and use case handlers
  - `com.quezap.infrastructure.aot.QuezapRuntimeHints` — JDK dynamic proxies for Spring AOP (`Repository`, `DataSource`), JPA entities, JJWT `KeysBridge`, profanity-filter dictionaries, and Apache Tika resources
  - `com.quezap.api.aot.QuezapApiRuntimeHints` — API-level DTO and Jackson hints
- **GraalVM Tracing Agent**: configured in conditional mode (`user-code-filter.json`) to merge runtime metadata captured during tests into `src/main/resources/META-INF/native-image`.

---

## ⚙️ Configuration

Configuration files live in `quezap-api/src/main/resources/application.yml` (+ profile overrides).

Key environment variables:
- **Database**: `APP_DB_URL`, `APP_DB_USERNAME`, `APP_DB_PASSWORD`, `SPRING_JPA_HIBERNATE_DDL_AUTO`
- **Object Storage (S3 / RustFS)**: `APP_S3_HOST`, `APP_S3_PORT`, `APP_S3_PUBLIC_ENDPOINT`, `APP_S3_BUCKET_NAME`, `APP_S3_ACCESS_KEY_ID`, `APP_S3_SECRET_ACCESS_KEY`
- **JWT**: `APP_JWT_SECRET_KEY` (hex key ≥ 32 bytes), `APP_JWT_ISSUER`

---

## 📚 API & CLI Summary

- **REST API**: endpoints under `/apiv1/**` (controllers in `quezap-api/src/main/java/com/quezap/api/v1/routes`)
- **CLI commands**:
  - List users: `users list`
  - Add user: `users add --name <name> --email <email>`
  - Delete user: `users delete --loginOrId <id-or-email>`

---

## 🤝 Contributing

- Format code with `./gradlew spotlessApply` before committing.
- Ensure Checkstyle (`checkstyleMain`, `checkstyleTest`) and NullAway (`-Pnullaway=true`) pass with zero warnings.
- Run the full test suite and AOT verification before opening a pull request.
