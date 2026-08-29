---
name: graalvm-spring-native
description: >-
  Best practices, AOT Runtime Hints, reflection registration, and troubleshooting for Spring Boot 3.5+
  applications compiled natively with Oracle GraalVM 25.
---

# GraalVM Spring Boot Native Image Guide

This skill provides guidelines and patterns for compiling and maintaining the `quezap-api` native binary.

## Key Rules for GraalVM Native Image in QueZap

### 1. Programmatic Runtime Hints
When adding new domain classes, DTOs, custom Jackson deserializers, or reflection-based validators:
- Register them in `com.quezap.application.aot.QuezapRuntimeHints` or `com.quezap.infrastructure.aot.QuezapRuntimeHints`.
- Include `MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS`, `MemberCategory.INVOKE_DECLARED_METHODS`, and `MemberCategory.DECLARED_FIELDS` when reflection is used by Jackson or Hibernate.

### 2. Spring AOP & Dynamic Proxies
- When interfaces are intercepted (such as `AuditAndEventInterceptor`), register dynamic proxy hints in `modules:infrastructure` AOT hints:
  ```java
  hints.proxies().registerJdkProxy(MyRepository.class, RepositoryProxy.class, ...);
  ```

### 3. NullAway & Strict Code Quality
Always ensure the build passes with NullAway enabled before attempting native compilation:
```bash
./gradlew clean test -Pnullaway=true
```

### 4. Native Compilation Execution
```bash
./gradlew :quezap-api:nativeCompile
./quezap-api/build/native/nativeCompile/quezap-api
```

