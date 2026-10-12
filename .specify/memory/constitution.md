# QueZap Constitution

Ce document régit les principes architecturaux, techniques et de qualité du projet **QueZap** (application de quiz interactif temps réel).

## Core Principles

### I. Découpage Hexagonal & Domain-Driven Design Strict (Backend)
- **`modules:domain`** : Métier pur. Zéro dépendance Spring/framework (standard library + JSpecify uniquement). Les invariants métier sont vérifiés systématiquement via `Domain.checkDomain(...)`. Les agrégats étendent `AggregateRoot<EntityId>`.
- **`modules:application`** : Ports applicatifs, Use Cases / Command Handlers orchestrés avec `UnitOfWorkEvents`. Pas de couplage HTTP/MVC.
- **`modules:infrastructure`** : Adaptateurs techniques (Spring Data JPA, Hibernate, adaptateurs InMemory, AWS S3 / MinIO, Argon2, JWT).
- **`quezap-api` / `quezap-cli`** : Adaptateurs primaires REST (Spring Boot, SSE `SessionSseService`) et CLI Spring Shell.

### II. Null Safety & Rigueur Typée (Backend & Frontend)
- **Backend (NullAway & JSpecify)** : Tout le code respecte `@NullMarked` et `@Nullable`. Compilation sous `-Pnullaway=true` avec avertissements traités en erreurs.
- **Frontend (Typage absolu & Zod)** : Zéro `any`, zéro `$any()`. Validation des schémas et formulaires via **Zod**.

### III. Synchronisation Client API sans Duplication (Frontend)
- Utilisation exclusive du client généré par Hey-API (`front/openapi-ts.config.ts`, `bun run codegen`).
- Interdiction formelle de coder des DTOs artisanaux ou des interfaces en doublon.
- Rôle d'Anti-Corruption Layer (ACL) dans les services : mapping direct vers `@quezap/domain/models`.
- Respect du typage strict Hey-API : `client.get<{ 200: MonDto }>` et jamais `client.get<MonDto>`.

### IV. Composants & Réactivité Angular 21
- Standalone components systématiques.
- Gestion d'état basée sur Angular Signals et Signal Stores (`@ngrx/signals`). Signal inputs `input()` / `output()`.
- Gestion stricte des fuites de mémoire RxJS : cycle de vie géré systématiquement via `takeUntilDestroyed(destroyRef)`.

### V. Compilation Native GraalVM 25
- Tout nouveau DTO, validateur réfléchi, convertisseur Jackson ou entité doit être déclaré dans les AOT Runtime Hints (`com.quezap.application.aot.QuezapRuntimeHints` ou `com.quezap.infrastructure.aot.QuezapRuntimeHints`).
- Proxys dynamiques enregistrés via `hints.proxies().registerJdkProxy(...)`.

## Quality Gates & Verification

- **Backend** :
  - Formatage : `./back/gradlew -p back spotlessApply`
  - Linters : `./back/gradlew -p back checkstyleMain checkstyleTest`
  - Tests & NullAway : `./back/gradlew -p back test -Pnullaway=true`
  - OpenAPI : `./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.GenerateOpenApiJsonTest"`
- **Frontend** :
  - Linters : `cd front && bun run lint:ts:fix && bun run lint:css:fix`
  - Tests Jest : `cd front && bun run test`
  - Build AOT : `cd front && bun run build`
- **Commits** : Conventional Commits (`feat`, `fix`, `refactor`, `test`, `chore`, etc. avec scopes `front`, `back`, `modelisation`).

## Governance

Toute nouvelle fonctionnalité implémentée via les spécifications `spec-kit` (`/speckit-*`) doit obligatoirement respecter cette constitution. Tout compromis technique doit être documenté et justifié.

**Version**: 1.0.0 | **Ratified**: 2026-10-12 | **Last Amended**: 2026-10-12
