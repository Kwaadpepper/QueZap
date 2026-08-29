---
name: ddd-hexagonal-spring
description: >-
  Domain-Driven Design (DDD) and Hexagonal Architecture rules and conventions for the QueZap Java backend.
---

# DDD & Hexagonal Architecture Guide for QueZap

## Architecture Layers & Dependency Rules

1. **`modules:domain` (Pure Domain)**:
   - Zero Spring / Framework dependencies (only `@Nullable` from JSpecify and Java standard library).
   - Invariants must be enforced inside constructor and state mutation methods using `Domain.checkDomain(...)`.
   - Aggregates extend `AggregateRoot<EntityId>` and handle their own lifecycle.
   - Ports (Repository and Directory interfaces) are defined here as Java interfaces.

2. **`modules:application` (Use Cases)**:
   - Contains ports (Input/Output records) and Handlers annotated with `@Usecase`.
   - Coordinates domain operations inside a `UnitOfWorkEvents`.
   - No HTTP or REST dependencies.

3. **`modules:infrastructure` (Adapters)**:
   - Technical implementations of repositories (JPA/Hibernate, InMemory).
   - External services (AWS S3, Argon2 password hasher, JWT service).
   - Spring domain event publishers.

4. **`quezap-api` (Driving REST Adapter)**:
   - Controllers handle HTTP requests, convert DTOs to Application Inputs, invoke Handlers, and map Outputs to HTTP responses.
   - Global exception handler maps domain and application exceptions to standard HTTP error codes.

