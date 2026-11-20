# Antigravity Agent Guidelines - QueZap Project

Bienvenue sur le projet **QueZap**, l'application web interactive de quiz en temps réel (écran enseignant/vidéoprojecteur + interfaces élèves mobiles synchronisées via Server-Sent Events).

Ce document définit les règles d'architecture, les conventions de code, les commandes clés et les **skills Antigravity** à mobiliser pour traiter chaque demande avec un maximum de performance et de fidélité au codebase.

---

## 🎯 Architecture Générale du Projet

Le dépôt est un monorepo structuré en deux sous-projets principaux :
- **`back/`** : Backend Spring Boot 3.5.7 (Java 25 / GraalVM 25) en Domain-Driven Design (DDD) et architecture hexagonale multi-modules Gradle.
- **`front/`** : Frontend Angular 21 (Standalone Components, Signals, `@ngrx/signals`, Tailwind CSS v4, PrimeNG v21).

### Infrastructure locale (Docker / Podman Compose)
- **PostgreSQL 17** (`localhost:5432`, base `quezap`, utilisateur `quezap`, mot de passe `quezap_password`)
- **MinIO S3** (`localhost:9190` API hôte -> `9000` conteneur, console `9191` -> `9001`, bucket `quezap-public-images`)
- **Application complète conteneurisée** : Accessible sur `http://localhost:8000` via Nginx frontend (`proxy_buffering off` SSE et proxy S3).

#### ⚠️ Pièges & Commandes Podman Rootless / Docker Compose
- **Socket Podman Rootless** : Sous Linux avec Podman en mode rootless, `/var/run/docker.sock` est inaccessible (erreur `Permission denied`). Exporter le socket utilisateur actif ou utiliser `podman-compose` :
  ```bash
  export DOCKER_HOST="unix://${XDG_RUNTIME_DIR:-/run/user/$(id -u)}/podman/podman.sock"
  docker-compose up -d postgres minio minio-init
  # OU alternativement avec podman-compose natif :
  podman-compose up -d postgres minio minio-init
  ```
- **Conflit de ports MinIO (9000 vs 9190)** : Sur beaucoup de machines hôtes, le port 9000 est déjà occupé. Le backend écoute en dev local sur `APP_S3_PORT=9190` (`APP_S3_HOST=localhost`). Le service `minio` dans `docker-compose.yml` publie impérativement `ports: ["9190:9000", "9191:9001"]` tout en conservant `expose: ["9000", "9001"]`.
- **E2E Playwright et URL cible** :
  - Sur conteneurs actifs (`docker-compose up`) : Playwright cible par défaut `http://localhost:8000`.
  - En dev local (`bun run start` sur port 4200) : passer la variable `PLAYWRIGHT_BASE_URL="http://localhost:4200" bun run test:e2e`.

---

## 🧭 Règles Backend (Java 25 / Spring Boot 3.5 / DDD)

### 1. Découpage Hexagonal Strict
- **`modules:domain`** : Cœur métier pur. **Zéro dépendance Spring/Framework** (uniquement la bibliothèque standard et `@Nullable`/`@NonNull` de JSpecify). Les invariants métier sont vérifiés dans les constructeurs et méthodes d'état via `Domain.checkDomain(...)`. Les agrégats étendent `AggregateRoot<EntityId>`. Les ports de dépôt et d'annuaire y sont définis sous forme d'interfaces Java.
- **`modules:application`** : Ports applicatifs (records Input/Output) et Handlers annotés avec `@Usecase`. Coordonne les opérations métier au sein d'un `UnitOfWorkEvents`. Aucune dépendance HTTP ou Spring MVC.
- **`modules:infrastructure`** : Adaptateurs techniques (Spring Data JPA, Hibernate, adaptateurs InMemory, AWS S3 SDK, Argon2, JWT, publication d'événements Spring).
- **`modules:shared`** : Primitives DDD transverses (identifiants typés, pagination, UUIDv7).
- **`quezap-api`** : Adaptateur primaire REST Spring Boot (contrôleurs sous `apiv1/...`, DTOs de transport, `GlobalExceptionHandler`, service SSE `SessionSseService`, AOT Runtime Hints).
- **`quezap-cli`** : Adaptateur primaire CLI Spring Shell pour l'administration et le seeding.

### 2. Zéro État Métier Volatile en Contrôleur & SSE Robuste
- **Persistance intégrale du cycle de vie** : Ne **jamais** stocker d'état métier affectant le déroulement, le calcul des scores ou les chronomètres (ex. `pausedAt`, `status`, `currentSlideIndex`) dans une variable mémoire ou `ConcurrentHashMap` d'un contrôleur REST. Tout état doit être porté par l'agrégat `Session` et persisté en BDD.
- **`SessionSseService` atomique & purge** : Le buffer d'historique SSE en mémoire doit synchroniser de manière atomique l'incrémentation du numéro de séquence et l'insertion dans le tampon borné. Purger explicitement la mémoire et fermer les `SseEmitter` morts lors de la fin de session (`endedAt != null`) ou après expiration.

### 3. S3, Transactions & Référence Counting
- **Suppression S3 post-commit** : Ne jamais supprimer un objet S3 (`QuestionPictureManager.removeByObjectKey`) au cours d'une transaction `@Transactional` avant son commit en base (risque de perte de média si la transaction rollbacke).
- **Reference counting obligatoire** : Avant toute suppression S3 (`removeByObjectKey`) lors de l'édition, duplication ou suppression de questions/quiz, vérifier qu'aucune autre entité (`QuestionJpaEntity`, `AnswerJpaEntity` issue d'un clonage) ne référence encore cette clé S3.
- **Purge BDD sélective dans les cron** : Dans les tâches planifiées (`@Scheduled` de `StagedPictureService`), ne purger en base via `deleteAllInBatch` **que** les enregistrements dont la suppression effective sur S3 a réussi (ou retourné 404). Ne jamais supprimer aveuglément les lignes en BDD en cas d'échec réseau S3 (pour éviter les fichiers orphelins sur le bucket).
- **Calcul SHA-256 via `DigestInputStream`** : Pour calculer le hash SHA-256 lors de l'upload S3, utiliser `DigestInputStream` en lecture directe. Interdiction formelle du couplage multi-threadé `PipedInputStream`/`PipedOutputStream` (risque avéré de deadlocks sous forte charge ou buffer saturé).

### 4. Null Safety, Spotless & Checkstyle
- **NullAway activé par défaut** : Tout code doit respecter les annotations JSpecify (`@NullMarked` au niveau paquet, `@Nullable` sur les champs optionnels). Le flag `-Pnullaway=true` traite les avertissements en erreurs de compilation (`-Werror`).
- **Spotless pre-commit obligatoire** : Google Java Format est vérifié par le hook Husky. Toujours appliquer le formateur avant de committer : `./back/gradlew -p back spotlessApply`.
- **Checkstyle & limite de 100 caractères** : `spotlessApply` ne coupe pas les longues chaînes dans les annotations Java (`@Schema(description = "...")`, `@Pattern(message = "...")`). Concaténer manuellement les chaînes littérales de plus de 100 caractères sur plusieurs lignes (`"..." + "..."`) pour valider `checkstyleMain` et `checkstyleTest`.
- **Commande de validation complète backend** :
  ```bash
  ./back/gradlew -p back spotlessApply checkstyleMain checkstyleTest test -Pnullaway=true
  ```

### 5. Mapping d'Erreurs HTTP (`GlobalExceptionHandler`)
- **Codes explicites vs chaînes anglaises** : Ne jamais déterminer un code HTTP (`404`, `409`, `400`) en recherchant des sous-chaînes de texte arbitraires dans `ex.getMessage()` (ex. `"does not exist"`). Utiliser une discrimination stricte basée sur des types d'exceptions précis, des codes d'erreur structurés (`ApplicationErrorCode`) ou des enums dédiés.

### 6. Compilation Native GraalVM 25
- Tout nouveau DTO, validateur réfléchi, convertisseur Jackson ou entité doit être déclaré dans les AOT Runtime Hints (`QuezapRuntimeHints_Application`, `QuezapRuntimeHints_Infrastructure` ou `QuezapApiRuntimeHints`).
- Les proxys dynamiques d'AOP ou de transactions nécessitent `hints.proxies().registerJdkProxy(...)`.
- Valider la configuration AOT sans lancer un build natif complet via :
  ```bash
  ./back/gradlew -p back :quezap-api:processAot :quezap-api:compileAotTestJava
  ```

---

## 🎨 Règles Frontend (Angular 21 / Tailwind CSS v4)

### 1. Composants, Signals & Réactivité
- **Standalone Components** systématiques (aucun NgModule).
- **Angular Signals & Signal Stores** : Utiliser `@ngrx/signals` pour la gestion d'état (`ActiveSessionStore`, `TimerStore`).
- **Noms uniques dans les SignalStores** : Chaque membre doit avoir un nom unique à travers `withState`, `withComputed` et `withMethods`. Ne jamais surcharger une propriété (interdiction des conflits provoquant le warning `@ngrx/signals: SignalStore members cannot be overridden`).
- **Props de composants** : Utiliser les Signal inputs `input()` / `input.required()` et `output()`.
- **Nettoyage RxJS** : Toute souscription asynchrone dans un composant doit être liée au cycle de vie via `takeUntilDestroyed(destroyRef)`.

### 2. Synchronisation Client API (Hey API & Zod)
- **Utilisation exclusive de `sdk.gen.ts`** :
  - Appeler directement les fonctions générées et typées (`login`, `forgottenPassword`, `find`, `create1`, `exportCsv`, `upload`, etc.).
  - Le compilateur TypeScript garantit ainsi les noms exacts (`identifier` vs `email`, `perPage` vs `pageSize`) et les formats de retour DTO.
- **Interdiction formelle des DTOs artisanaux et de `client.post<Dto>` brut** :
  - Ne jamais coder d'interfaces DTO ou `*ApiResponse` artisanales à la main.
  - En cas d'usage exceptionnel des méthodes bas niveau de `@hey-api/client-angular`, typer obligatoirement avec le dictionnaire de statuts HTTP : `client.post<{ 200: MonDto, 201: MonDto }>({...})`. Ne jamais écrire `client.post<MonDto>` (TS inférerait `MonDto[keyof MonDto]`).
- **Interdiction totale d'injecter `HttpClient` dans les composants/pages** :
  - Tout appel réseau (y compris le téléchargement de Blob CSV) doit être encapsulé dans un service métier dédié s'appuyant sur le SDK généré.
- **Zéro `any` & Zod** : L'utilisation de `any` ou du helper `$any()` est formellement proscrite. Exploiter les schémas Zod générés dans `src/app/core/api-client/zod.gen.ts`.
- **Régénération du SDK** : Après toute modification des DTOs ou contrôleurs backend :
  ```bash
  ./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.GenerateOpenApiJsonTest"
  cd front && bun run codegen
  ```

### 3. Signal Forms & Validation
- **`[formField]` + `[fieldState]` obligatoires ensemble** : Toute balise `<input>` exploitant la directive `quizzFieldError` DOIT impérativement lier à la fois `[formField]="monForm.champ"` et `[fieldState]="monForm.champ"`. L'omission de `[formField]` laisse le champ visuellement actif mais déconnecté du modèle de données sous-jacent.

### 4. Bonnes Pratiques de Rendu & Accessibilité
- **Opérateur `??` vs `||` pour le chiffre `0`** : Utiliser systématiquement l'opérateur de coalescence des nuls `??` (et jamais `||`) pour les valeurs par défaut numériques (`points`, `rank`, `score`, `timer`, etc.) où `0` est une valeur métier valide (évite le remplacement fallacieux de 0 par la valeur par défaut).
- **Classes CSS HTML & bindings Angular** : Ne jamais dupliquer l'attribut HTML statique `class="..."` sur une même balise, et éviter la coexistence conflictuelle de `class="..."` avec `[class]="expr"`. Préférer `[ngClass]` ou des bindings booléens granulaires `[class.ma-classe]="condition"`.
- **Interdiction de `pFocusTrap` sur `<main>` avec `overlayAppendTo: 'body'`** : Ne jamais poser `pFocusTrap` sur la balise `<main>` globale lorsque PrimeNG rattache ses modales et menus déroulants au `<body>` (`p-select`, `p-confirmdialog`, `p-image`). Cela enfermerait le focus dans le main et rendrait les overlays inaccessibles au clavier. Réserver le piège de focus au conteneur de chaque modale.

### 5. Résilience Élève, États & Erreurs
- **Persistance élève via `localStorage` scopé par `sessionId`** : Ne jamais stocker la reprise élève (`submittedChoice`, `finalResults`, `participantName`) uniquement dans `sessionStorage`. Utiliser un `localStorage` préfixé par l'identifiant de session, avec nettoyage explicite à la fin ou lors du changement de session, pour survivre aux fermetures accidentelles d'onglets.
- **Zéro KPI synthétique codé en dur** : Ne jamais générer de métriques artificielles ou calculs fictifs dans l'interface (ex. `count * 24` ou `78%`). Les tableaux de bord et bilans doivent refléter fidèlement les données réelles du backend.
- **Zéro erreur silencieuse** : Ne jamais ignorer les échecs utilisateur (`isFailure(output)`, codes HTTP 4xx/5xx). Fournir un feedback explicite (toast PrimeNG `error`, message d'état clair) et stopper les animations d'attente (spinners).

### 6. PWA & Configuration de Build
- **Service Worker PWA (`ngsw-config.json`)** :
  - Spécifier `navigationUrls` en excluant explicitement les routes d'API et d'images : `!/apiv1/**` et `!/quezap-public-images/**`.
  - Ne jamais placer en stratégie de mise en cache `performance` des endpoints de gestion CRUD (ex. `/apiv1/themes/**`, `/apiv1/quezaps/**`) ; réserver la fraîcheur maximale au backend.
- **Dépendances CommonJS (`angular.json`)** : Déclarer `"allowedCommonJsDependencies": ["qrcode"]` sous `architect.build.options` pour éviter les avertissements d'optimisation esbuild.

---

## ⚡ Skills Antigravity Recommandés pour la Performance

| Skill | Localisation | Quand l'activer |
| :--- | :--- | :--- |
| **`backend-testing`** | [SKILL.md](file://.agents/skills/backend-testing/SKILL.md) | Tests unitaires ciblés, vérifications NullAway, Spotless, Checkstyle, JaCoCo, génération OpenAPI. |
| **`frontend-testing`** | [SKILL.md](file://.agents/skills/frontend-testing/SKILL.md) | Tests Jest ciblés sur un composant/store, linters (ESLint flat config, Stylelint), sync codegen. |
| **`ddd-hexagonal-spring`** | [SKILL.md](file://.agents/skills/ddd-hexagonal-spring/SKILL.md) | Ajout/modification d'agrégats, invariants, Use Cases, ports et adaptateurs JPA/InMemory. |
| **`graalvm-spring-native`** | [SKILL.md](file://.agents/skills/graalvm-spring-native/SKILL.md) | Ajout de runtime hints AOT, configuration de proxys dynamiques, compilation binaire native. |
| **`e2e-playwright`** | [SKILL.md](file://.agents/skills/e2e-playwright/SKILL.md) | Simulation complète de session de classe (1 écran enseignant + N onglets mobiles élèves via SSE). |

---

## 🛠️ Commandes Utiles & Boucle Rapide

### Backend (`back/`)
```bash
# Test rapide d'une classe unique (sans rebuilder tout le projet)
./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.routes.AuthControllerTest"

# Vérification qualité complète avant commit (Spotless + Checkstyle + NullAway + Tests)
./back/gradlew -p back spotlessApply checkstyleMain checkstyleTest test -Pnullaway=true

# Formater le code Java
./back/gradlew -p back spotlessApply

# Vérification rapide de la compilation AOT GraalVM
./back/gradlew -p back :quezap-api:processAot :quezap-api:compileAotTestJava

# Générer l'openapi.json pour le frontend
./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.GenerateOpenApiJsonTest"

# Lancer l'API Spring Boot
./back/gradlew -p back :quezap-api:bootRun

# Lancer le shell d'administration
./back/gradlew -p back :quezap-cli:bootRun --console=plain
```

### Frontend (`front/`)
```bash
# Test Jest ciblé sur un fichier spec
cd front && bun run test -- src/app/features/admin/themes/components/theme-card/theme-card.spec.ts

# Régénérer le client TypeScript OpenAPI après changement backend
cd front && bun run codegen

# Linter TypeScript & CSS
cd front && bun run lint:ts:fix && bun run lint:css:fix

# Compilation AOT Angular (obligatoire avant commit ou build Docker)
cd front && bun run build

# Validation qualité complète frontend (Linters + Tests + Compilation AOT)
cd front && bun run lint && bun run test && bun run build

# Lancer la suite de tests E2E Playwright (sur environnement local port 4200)
PLAYWRIGHT_BASE_URL="http://localhost:4200" bun run test:e2e

# Lancer le serveur de développement Angular
cd front && bun run start
```

---

## 📝 Conventions de Commits (Conventional Commits)

Les commits sont contrôlés par Husky et Commitlint (`commitlint.config.cjs`) :
- **Format** : `<type>(<scope>): <sujet>` (sujet impératif, sans point final, minuscules, < 50 caractères).
- **Types autorisés** : `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `ci`.
- **Scopes autorisés** :
  - `front` : pour les modifications du frontend Angular.
  - `back` : pour les modifications du backend Spring Boot.
  - `modelisation` : pour les modifications conceptuelles ou transverses.

---

## 🧪 Pièges E2E réels (suite Playwright live)

- **`<label><input>`** : ne jamais écouter `(click)` sur le composant parent (2 clics par action utilisateur → double soumission) ; utiliser l'`output()` du composant déclenché depuis l'`<input>`, et garder une garde anti-double-envoi dans `question-submission.ts`.
- **Pagination** : `perPage <= 50` côté frontend, sinon `400`.
- **`GlobalExceptionHandler`** : ignorer `AsyncRequestNotUsableException`/SSE `Broken pipe` ; mapper `BadPaginationException` et `InvalidFileException` en `400`.
- **Nginx** : `docker-compose restart frontend` après recréation de `backend` (cache DNS Nginx).
