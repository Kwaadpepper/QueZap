# Audit Backend Périphérie — QueZap (`back/`)

Audit en lecture seule des contrôleurs hors `sessions/`/`quezaps/`/`pictures/`, de la sécurité (`SecurityConfig`, JWT, secrets), des services `SessionSseService` et `SessionReportService`, de `GlobalExceptionHandler`, de `quezap-cli`, des `RuntimeHints` GraalVM et de la configuration Gradle.

---

### P-01 — IDOR critique et contournement de l'ancien mot de passe sur `/apiv1/users`
- **Sévérité** : Critique
- **Localisation** :
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/users/UserController.java:93-132`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/dto/request/users/UpdatePasswordRequest.java:5-7`
  - `back/modules/application/src/main/java/com/quezap/application/usecases/users/UpdateUserPasswordHandler.java:35-47`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/security/JwtAuthenticationFilter.java:56-58`
- **Description** :
  - `JwtAuthenticationFilter` attribue uniformément `SimpleGrantedAuthority("ROLE_USER")` à tout JWT d'accès valide.
  - Dans `UserController`, `getById` (`GET /apiv1/users/{id}`), `list` (`GET /apiv1/users`), `updatePassword` (`PUT /apiv1/users/{id}/password`) et `delete` (`DELETE /apiv1/users/{id}`) n'injectent pas `Authentication` et ne vérifient pas que l'utilisateur authentifié correspond à `{id}` ou dispose d'un rôle administrateur.
  - De plus, `updatePassword(@PathVariable UUID id, @Valid @RequestBody UpdatePasswordRequest request)` reçoit `request.oldPassword()`, mais l'ignore totalement à la ligne 120-121 (`new UpdateUserPassword.Input.Id(new UserId(id), new RawPassword(request.newPassword()))`), et `UpdateUserPasswordHandler.handle` ne vérifie pas non plus l'ancien mot de passe. N'importe quel utilisateur authentifié peut lister tous les comptes, modifier le mot de passe d'un autre enseignant sans connaître son ancien mot de passe, ou supprimer n'importe quel compte.
- **Correction proposée** :
  - Vérifier l'identité du principal (`UUID.fromString(authentication.getName()).equals(id)`) ou un rôle `ROLE_ADMIN` sur `getById`, `updatePassword`, `delete`, et restreindre `list` à un rôle administrateur (ou exposer uniquement le profil courant via `/apiv1/auth/me`).
  - Transmettre `oldPassword` dans `UpdateUserPassword.Input` et vérifier `passwordHasher.verify(oldPassword, credential.getHashedPassword())` dans `UpdateUserPasswordHandler` (lever une erreur dédiée en cas d'échec).
- **Critère d'acceptation testable** :
  - Un utilisateur authentifié `A` appelant `PUT /apiv1/users/{B}/password`, `DELETE /apiv1/users/{B}` ou `GET /apiv1/users/{B}` reçoit `403 Forbidden`.
  - Un utilisateur `A` appelant `PUT /apiv1/users/{A}/password` avec un `oldPassword` erroné reçoit `400`/`403` et son mot de passe reste inchangé.

---

### P-02 — Secrets par défaut dangereux en configuration (y compris `prod`) et `ddl-auto: update` sans migrations
- **Sévérité** : Critique
- **Localisation** :
  - `back/quezap-api/src/main/resources/application.yml:14-20,48,57-62`
  - `back/quezap-api/src/main/resources/application-prod.yml:18-19`
  - `back/quezap-cli/src/main/resources/application.yml:5-7,31,42-43`
  - `back/modules/infrastructure/src/main/java/com/quezap/infrastructure/adapter/services/config/JwtServiceConfig.java:18-29`
- **Description** :
  - Des secrets de repli en clair sont définis par défaut : `jwt.signing-key: ${APP_JWT_SECRET_KEY:4a3c4e58311739e4a396ad7a38a5454ad65cfde67b0fef9aeb351cbb3c478fef}`, `pictures-s3.secret-access-key: ${APP_S3_SECRET_ACCESS_KEY:LocalPassword}` (également présent dans `application-prod.yml`), et `spring.datasource.password: ${APP_DB_PASSWORD:quezap_password}` (et `quezap` côté CLI). Si `APP_JWT_SECRET_KEY` n'est pas fournie en production, l'API démarre silencieusement avec une clé HMAC publique issue du dépôt Git, permettant à quiconque de forger des JWT d'accès et de rafraîchissement.
  - `application.yml:20` conserve `spring.jpa.hibernate.ddl-auto: update` sans profil de désactivation pour `prod` et sans outil de migration (`flyway` / `liquibase` absents de `back/gradle/libs.versions.toml` et `back/build.gradle`).
  - `session-code.seed: 12345` (`application.yml:48`) est statique.
- **Correction proposée** :
  - Retirer toutes les valeurs par défaut des secrets (`jwt.signing-key: ${APP_JWT_SECRET_KEY}`, `APP_DB_PASSWORD`, `APP_S3_SECRET_ACCESS_KEY`) dans `application-prod.yml` et déplacer les valeurs locales uniquement vers `application-dev.yml` / `application-local.yml`.
  - Ajouter une validation au démarrage dans `JwtServiceConfig` (longueur minimale ≥ 32 octets, rejet de la clé de dev connue hors profil `dev`/`test`).
  - Passer `spring.jpa.hibernate.ddl-auto` à `validate` en production et introduire Flyway/Liquibase pour le schéma PostgreSQL.
- **Critère d'acceptation testable** :
  - Démarrer `:quezap-api` avec `--spring.profiles.active=prod` sans `APP_JWT_SECRET_KEY` (ou avec une clé < 32 octets) échoue immédiatement au bootstrap Spring.
  - Avec `ddl-auto: validate` en `prod`, toute divergence de schéma empêche le démarrage plutôt que d'exécuter un `ALTER TABLE` automatique.

---

### P-03 — Absence totale d'ownership sur les mutations de thèmes et de questions
- **Sévérité** : Haute
- **Localisation** :
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/themes/RenameThemeController.java:25-31`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/themes/DeleteThemeController.java:22-27`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/questions/DeleteQuestionController.java:18-24`
  - `back/modules/domain/src/main/java/com/quezap/domain/models/entities/Theme.java:12-14`
  - `back/modules/domain/src/main/java/com/quezap/domain/models/entities/Question.java:21-29`
- **Description** :
  - Les routes `PATCH /apiv1/themes/{id}`, `DELETE /apiv1/themes/{id}` et `DELETE /apiv1/questions/{id}` ne requièrent qu'un JWT valide (`authenticated()` dans `SecurityConfig.java:77-78`) sans vérifier l'auteur/propriétaire de la ressource ni un rôle administrateur.
  - Les entités domaine `Theme` et `Question` ne stockent d'ailleurs aucun `ownerId`/`authorId`. Un enseignant `B` peut renommer ou supprimer les thèmes et questions créés par un enseignant `A`.
- **Correction proposée** :
  - Si les thèmes/questions sont scopés par créateur, ajouter un `ownerId` (`UserId`) sur `Question` (et/ou `Theme`), le propager via les ports applicatifs depuis `Authentication.getName()`, et refuser (`403 Forbidden`) toute mutation/suppression par un non-propriétaire (hors `ROLE_ADMIN`). Si les thèmes sont globaux/partagés, restreindre `POST`/`PATCH`/`DELETE /apiv1/themes/**` à `ROLE_ADMIN`.
- **Critère d'acceptation testable** :
  - Un enseignant `B` tentant `DELETE /apiv1/questions/{idDeA}` ou `PATCH /apiv1/themes/{id}` (si réservé admin/propriétaire) reçoit `403 Forbidden` et la ressource reste intacte en base.

---

### P-04 — Exposition publique de `/apiv1/questions/**`, `/actuator/**` et Swagger en `permitAll()`
- **Sévérité** : Haute
- **Localisation** :
  - `back/quezap-api/src/main/java/com/quezap/api/v1/security/SecurityConfig.java:68-76`
- **Description** :
  - Ligne 68-69 : `.requestMatchers(HttpMethod.GET, "/apiv1/questions/**").permitAll()` expose publiquement la banque de questions sans authentification.
  - Lignes 70-76 : `"/v3/api-docs/**"`, `"/swagger-ui/**"`, `"/swagger-ui.html"` et `"/actuator/**"` sont tous en `.permitAll()`. Autoriser `/actuator/**` en bloc ouvre l'ensemble des endpoints Actuator activés sans authentification.
- **Correction proposée** :
  - Retirer `HttpMethod.GET, "/apiv1/questions/**"` des routes `permitAll()` (les questions d'une session active étant déjà diffusées via les DTOs sécurisés sans bonne réponse `SafeQuestionDto` sur `/apiv1/sessions/**`).
  - Restreindre Actuator à `/actuator/health` (et `/actuator/health/**` si probes k8s) en `permitAll()` et protéger le reste de `/actuator/**` par rôle `ROLE_ADMIN`. Désactiver Swagger/OpenAPI en profil `prod` (`springdoc.api-docs.enabled: false`, `springdoc.swagger-ui.enabled: false`).
- **Critère d'acceptation testable** :
  - `GET /apiv1/questions` sans header `Authorization` retourne `401 Unauthorized`.
  - `GET /actuator/env` ou `/actuator/beans` (et `/v3/api-docs` en `prod`) retourne `401`/`404` pour un client anonyme, tandis que `GET /actuator/health` retourne `200 OK`.

---

### P-05 — CORS wildcard (`*`) combiné à `allowCredentials(true)` et absence de cookies `HttpOnly`
- **Sévérité** : Haute
- **Localisation** :
  - `back/quezap-api/src/main/java/com/quezap/api/v1/security/SecurityConfig.java:83-94`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/auth/AuthController.java:102-107,159-160`
- **Description** :
  - `SecurityConfig.corsConfigurationSource()` configure `setAllowedOriginPatterns(List.of("*"))` avec `setAllowCredentials(true)` (lignes 86 et 90) et `setExposedHeaders(List.of("*"))` (ligne 89). Spring reflète alors n'importe quel en-tête `Origin` dans `Access-Control-Allow-Origin` avec `Access-Control-Allow-Credentials: true`, permettant à un domaine tiers arbitraire d'effectuer des requêtes cross-origin authentifiées.
  - De plus, `AuthController.login` et `refresh` renvoient `accessToken` et `refreshToken` (validité 30 jours) dans le payload JSON sans utiliser de cookie `HttpOnly`, `Secure`, `SameSite=Strict/Lax`, exposant le refresh token au vol par XSS côté navigateur.
- **Correction proposée** :
  - Externaliser `app.cors.allowed-origins` dans `application*.yml` (liste blanche explicite en production, pas de `*` avec `allowCredentials(true)`), et lister explicitement les headers autorisés/exposés (`Authorization`, `Content-Type`, `X-Participation-Token`, `Content-Disposition`).
  - Porter le `refreshToken` dans un cookie `HttpOnly; Secure; SameSite=Lax` (path `/apiv1/auth`) ou documenter/sécuriser strictement le stockage si Bearer pur (avec CSRF activé si passage aux cookies).
- **Critère d'acceptation testable** :
  - Une requête `OPTIONS` / `GET` avec `Origin: https://evil.example` ne reçoit pas `Access-Control-Allow-Origin: https://evil.example` ni `Access-Control-Allow-Credentials: true`.

---

### P-06 — Refresh tokens non révocables, validation laxiste du claim `type`, endpoints auth factices et absence de rate limiting
- **Sévérité** : Haute
- **Localisation** :
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/auth/AuthController.java:62-77,129-198`
- **Description** :
  - **Claim `type` optionnel sur refresh** (`AuthController.java:133-136`) : `if (tokenType != null && !"refresh".equals(tokenType))` — un JWT ne contenant pas le claim `type` est accepté comme refresh token ; il faut `!"refresh".equals(tokenType)`.
  - **Aucune révocation** (`AuthController.java:166-174`) : `logout` se contente de parser le token sans l'invalider. Comme le refresh token a une durée de vie de 30 jours (`95, 152`) et aucune table/cache de révocation ou numéro de version utilisateur (`tokenVersion`) n'existe, un refresh token reste valide 30 jours même après `logout`, `updatePassword` ou `delete`.
  - **Endpoints no-op** (`AuthController.java:176-198`) : `POST /apiv1/auth/forgotten-password`, `GET /apiv1/auth/verify-reset-token`, `POST /apiv1/auth/reset-password` et `POST /apiv1/auth/activate` renvoient `202`/`200` avec des messages de succès (`"Password successfully updated."`, `"Account successfully activated."`) sans rien faire en base.
  - **Pas de rate limiting** sur `POST /apiv1/auth/login`, `POST /apiv1/auth/refresh`, `POST /apiv1/users` (vulnérable au brute-force Argon2 / DoS CPU).
- **Correction proposée** :
  - Exiger `!"refresh".equals(tokenType)` dans `refresh()`.
  - Implémenter la rotation persistée des refresh tokens (table `refresh_tokens` avec `jti`, `revokedAt`, détection de réutilisation) et une vérification de révocation/version au `refresh` et `logout`.
  - Soit implémenter réellement les flux reset/activation avec jetons signés/stockés à usage unique, soit retirer ces routes ou retourner `501 Not Implemented` tant qu'elles sont inactives.
  - Ajouter un rate limiting (Bucket4j / filtre applicatif ou reverse-proxy + test d'intégration) sur `/apiv1/auth/login`, `/apiv1/auth/refresh` et `POST /apiv1/users`.
- **Critère d'acceptation testable** :
  - Un JWT sans claim `type` envoyé à `POST /apiv1/auth/refresh` reçoit `401 Unauthorized`.
  - Après `POST /apiv1/auth/logout` (ou réutilisation d'un refresh token déjà tourné), un nouvel appel à `/apiv1/auth/refresh` avec ce token retourne `401 Unauthorized`.
  - Des tentatives répétées de login échouées au-delà du seuil configuré reçoivent `429 Too Many Requests`.

---

### P-07 — Violation hexagonale et logique métier dans `AuthController` et `UserController`
- **Sévérité** : Haute
- **Localisation** :
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/auth/AuthController.java:31,43-47,62-164`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/users/UserController.java:49,65,71-105`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/dto/request/users/CreateUserRequest.java:24-69`
- **Description** :
  - `AuthController` injecte directement `UserJpaRepository` (couche `infrastructure`), `CredentialRepository` (port `domain`), `IdentifierHasher`, `PasswordHasher` et `JwtService`, contournant `modules:application` et `UsecaseExecutor`.
  - `UserController` injecte directement `UserRepository` (`UserController.java:49,96`) pour `getById`.
  - `UserController.create` et `CreateUserRequest` (`CreateUserRequest.java:46-69`) génèrent silencieusement un mot de passe aléatoire de 24 caractères si `password` est absent/blanc sans jamais le renvoyer ni exiger un vrai mot de passe utilisateur lors de l'inscription publique (`POST /apiv1/users` est `permitAll()`).
- **Correction proposée** :
  - Extraire l'authentification (`LoginUsecase`, `RefreshTokenUsecase`, `GetCurrentUserUsecase`) et la lecture utilisateur (`GetUserByIdUsecase`) dans `modules:application` afin que les contrôleurs ne dépendent plus de `UserJpaRepository` ni de `UserRepository`.
  - Rendre `password` obligatoire (`@NotBlank`) sur l'inscription API publique `CreateUserRequest` et supprimer `generateSecurePassword()` du DTO.
- **Critère d'acceptation testable** :
  - Aucun contrôleur sous `com.quezap.api.v1.routes.auth` ou `users` n'importe `*JpaRepository` ou `domain.ports.repositories.*`.
  - `POST /apiv1/users` avec un mot de passe null ou vide retourne `400 Bad Request`.

---

### P-08 — `GlobalExceptionHandler` : exceptions domaine/HTTP non interceptées (500) et mapping de statut HTTP par sous-chaîne défaillant
- **Sévérité** : Haute
- **Localisation** :
  - `back/quezap-api/src/main/java/com/quezap/api/errors/GlobalExceptionHandler.java:18-84`
  - `back/quezap-api/src/main/java/com/quezap/api/errors/ErrorResonseDto.java:7`
  - `back/modules/application/src/main/java/com/quezap/application/exceptions/themes/RemoveThemeError.java:33`
  - `back/modules/application/src/main/java/com/quezap/application/exceptions/themes/RenameThemeError.java:59`
  - `back/modules/application/src/main/java/com/quezap/application/exceptions/questions/AddQuestionError.java:163`
  - `back/modules/application/src/main/java/com/quezap/application/exceptions/questions/DeleteQuestionError.java:189`
- **Description** :
  - `GlobalExceptionHandler.handleApplicationConstraintException` (`32-43`) devine le code HTTP (`404`/`409`/`400`) en testant `lower.contains("not found") || lower.contains("no such") || lower.contains("introuvable")`. Or `RemoveThemeError.THEME_DOES_NOT_EXISTS` (`"No theme exists with the provided identifier"`), `RenameThemeError.THEME_DOES_NOT_EXISTS` (`"No theme exists with the provided identifier"`), `AddQuestionError.THEME_DOES_NOT_EXISTS` (`"The specified theme does not exist"`) et `DeleteQuestionError.QUESTION_NOT_FOUND` (`"The specified question does not exist"`) ne contiennent aucune de ces trois sous-chaînes et renvoient donc **400 Bad Request** au lieu de **404 Not Found**.
  - Aucun `@ExceptionHandler(Exception.class)` générique n'est défini, et `IllegalDomainStateException` (qui étend `IllegalStateException`, pas `IllegalArgumentException`) n'est pas interceptée. Ainsi, une validation domaine échouant au binding (`StringToThemeName`, `SearchQuery`, `ThemeNameDeserializer`, `RawPassword`, `RawIdentifier`, etc.) ou une `DataIntegrityViolationException` / `HttpMessageNotReadableException` remonte jusqu'au `/error` par défaut de Spring Boot (souvent en `500 Internal Server Error` au lieu de `400`/`409`).
  - `handleIllegalArgumentException` (`76-83`) renvoie `ex.getMessage()` brut au client ; `ErrorResonseDto` contient une faute d'orthographe (`ErrorResponseDto`).
- **Correction proposée** :
  - Ajouter une propriété de catégorie ou `HttpStatus` / type sémantique (`NOT_FOUND`, `CONFLICT`, `VALIDATION`, `FORBIDDEN`) sur `ApplicationErrorCode` (ou une interface dans `application`) plutôt que du parsing fragile sur les chaînes de message.
  - Ajouter des handlers pour `IllegalDomainStateException` (`400 Bad Request`), `HttpMessageNotReadableException` / `MethodArgumentTypeMismatchException` / `ConstraintViolationException` (`400 Bad Request`), `DataIntegrityViolationException` (`409 Conflict` sans détails SQL), et un fallback `@ExceptionHandler(Exception.class)` journalisant la stacktrace côté serveur et retournant un `500` générique sans détail technique.
  - Renommer `ErrorResonseDto` en `ErrorResponseDto`.
- **Critère d'acceptation testable** :
  - `DELETE /apiv1/themes/{uuidInexistant}`, `PATCH /apiv1/themes/{uuidInexistant}` et `DELETE /apiv1/questions/{uuidInexistant}` retournent `404 Not Found` avec le code applicatif JSON.
  - Une requête `GET /apiv1/themes?search=%20` ou `POST /apiv1/themes` avec `{"name": "a"}` retourne `400 Bad Request` au format `ErrorResponseDto` (pas `500`).
  - Une exception interne simulée retourne `500` avec un message générique sans stacktrace ni nom de classe.

---

### P-09 — `SessionSseService` : fuites mémoire, courses concurrentes sur l'historique, nettoyage incomplet des emitters et mono-instance
- **Sévérité** : Haute
- **Localisation** :
  - `back/quezap-api/src/main/java/com/quezap/api/v1/services/SessionSseService.java:22,31-104,72-75,93-95,163-165`
- **Description** :
  - **Fuites mémoire** : `sessionSequences` (`ConcurrentHashMap<UUID, AtomicLong>`) et `eventHistory` (`ConcurrentHashMap<UUID, CopyOnWriteArrayList<StoredEvent>>`) ne sont jamais purgées (ni à la fin de session `SESSION_ENDED`, ni par TTL), conservant indéfiniment les états et payloads de toutes les sessions passées.
  - **Race condition sur `eventHistory`** (`98-104`) : `nextSequence(sessionId)` puis `history.add(...)` puis `while (history.size() > MAX_HISTORY_SIZE) history.remove(0)` ne sont pas atomiques : deux appels concurrents à `broadcast()` peuvent insérer les événements dans le désordre ou dépasser/faire échouer la boucle de trim. De plus `CopyOnWriteArrayList` recopie tout le tableau à chaque ajout/suppression.
  - **Emitters non fermés/retirés en erreur** : en échec de l'envoi initial `CONNECTED` (`72-75`), `removeEmitter` est appelé mais pas `emitter.completeWithError(e)`/`complete()` ; dans `sendToEmitter` (`93-95`) et `sendStoredEventToEmitter` (`163-165`), les exceptions sont ignorées sans retirer l'emitter mort ni appeler `complete()`.
  - **Mono-instance** : `emitters`, `sessionSequences` et `eventHistory` sont en mémoire JVM locale : toute montée à l'échelle horizontale (>1 réplica sans sticky sessions + pub/sub Redis/PG) casse le streaming SSE et la reprise `Last-Event-ID`.
  - **Architecture Spring** : `@EnableScheduling` est posé sur le `@Service` lui-même (`22`) au lieu d'une classe `@Configuration`.
- **Correction proposée** :
  - Synchroniser par session (`compute` ou verrou par `sessionId`) la génération de séquence + ajout/éviction dans un `ArrayDeque<StoredEvent>` borné (ou buffer circulaire).
  - Ajouter une méthode `clearSession(UUID sessionId)` appelée à la clôture de session (ou après un court délai de grâce pour la reconnexion finale) et une éviction par TTL.
  - Compléter/supprimer l'emitter sur toute erreur d'envoi (`registerEmitter`, `sendToEmitter`, `sendStoredEventToEmitter`).
  - Déplacer `@EnableScheduling` vers une classe `@Configuration` dédiée et documenter/prévoir l'adaptateur pub/sub Redis/PostgreSQL si déploiement multi-instances.
- **Critère d'acceptation testable** :
  - Un test concurrent multi-threads appelant `broadcast()` sur la même session vérifie que `eventHistory` conserve strictement les `MAX_HISTORY_SIZE` derniers événements dans l'ordre croissant strict des `sequence`.
  - Après appel à `clearSession(sessionId)` (ou expiration TTL), `eventHistory`, `sessionSequences` et `emitters` ne contiennent plus l'entrée `sessionId`.

---

### P-10 — `SessionReportService` : encodage CSV sans `charset=UTF-8`/BOM, edge case `\n=` et complexité $O(P \times A)$
- **Sévérité** : Moyenne
- **Localisation** :
  - `back/quezap-api/src/main/java/com/quezap/api/v1/services/SessionReportService.java:28-90,133-268`
- **Description** :
  - `exportCsv` (`86-89`) renvoie une `ResponseEntity<String>` avec `Content-Disposition`, sans fixer explicitement `Content-Type: text/csv; charset=UTF-8` ni préfixer le BOM UTF-8 (`\uFEFF`), provoquant un mauvais décodage des accents (`Bonnes Réponses`, `Taux de Réussite`, pseudos) à l'ouverture directe sous Excel.
  - Dans `escapeCsvCell` (`33-45`), si la valeur commence par `\n=CMD()`, `stripLeading()` retire `\n` et préfixe `'` avant `\n` (`'\n=CMD()`), laissant la deuxième ligne débuter par `=`.
  - `exportCsv` (`63-71`) et `buildSessionReport` (`133-143`, `165-257`) re-filtrent `sessionJpa.getAnswers()` à chaque participant et à chaque question ($O(P \times A)$ et $O(Q \times A)$).
- **Correction proposée** :
  - Préfixer `\uFEFF` dans le CSV et définir explicitement `.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))`.
  - Préfixer `'` si le premier caractère (ou le caractère après `\n`/`\r`) est dans `= + - @ \t \r |`, ou normaliser/échapper les retours à la ligne dans les pseudos.
  - Indexer `sessionJpa.getAnswers()` une seule fois par `participantName` et par `slideIndex` avant les boucles.
- **Critère d'acceptation testable** :
  - `GET /apiv1/sessions/{id}/export/csv` retourne l'en-tête `Content-Type: text/csv;charset=UTF-8`, un corps débutant par `\uFEFF` et les cellules contenant `=`, `+`, `-`, `@`, `\t`, `\r`, `|` ou `\n=` neutralisées.

---

### P-11 — Validation DTO (`PaginationValidator`, `AddQuestionDto`), codes HTTP REST et gestion d'UUID invalide sur `/me`
- **Sévérité** : Moyenne
- **Localisation** :
  - `back/quezap-api/src/main/java/com/quezap/api/v1/validation/PaginationValidator.java:100-119`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/dto/request/questions/AddQuestionDto.java:37-57`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/dto/request/themes/CreateThemeRequest.java:7`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/themes/AddThemeController.java:26-27`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/themes/RenameThemeController.java:25-26`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/themes/DeleteThemeController.java:22-23`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/questions/AddQuestionController.java:46,68,96`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/questions/DeleteQuestionController.java:18-19`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/routes/auth/AuthController.java:115-120`
- **Description** :
  - `PaginationValidator.isValid` (`118`) retourne `hasPageRange || hasIndexRange || (!hasPageRange && !hasIndexRange)`, ce qui vaut toujours `true` (tautologie) : les plages partielles (`?page=2` seul, `?from=0` seul) ou la combinaison avec `offset`/`limit` ne sont pas rejetées par Bean Validation.
  - `AddQuestionDto` (`37-57`) est une `sealed interface` mais `BinaryDto` et `QuizzDto` omettent `implements AddQuestionDto` ; `question` et `answer` n'ont pas `@NotBlank`, et `CreateThemeRequest` / `AddQuestionDto` utilisent `jakarta.annotation.Nonnull` (non évalué par Jakarta Bean Validation) au lieu de `@NotNull`.
  - Plusieurs endpoints retournent `200 OK` par défaut au lieu des statuts REST attendus : `POST /apiv1/themes` et `POST /apiv1/questions/*` (`201 Created` manquant), `PATCH /apiv1/themes/{id}`, `DELETE /apiv1/themes/{id}` et `DELETE /apiv1/questions/{id}` (`204 No Content` manquant, alors que `UserController.updatePassword`/`delete` utilisent bien `@ResponseStatus(HttpStatus.NO_CONTENT)`).
  - `AuthController.me` (`120`) appelle `UUID.fromString(authentication.getName())` sans `try/catch` (produisant un `400` au lieu d'un `401` si `sub` n'est pas un UUID).
- **Correction proposée** :
  - Corriger `PaginationValidator` pour valider les 3 modes (`page/perPage`, `from/to`, `offset/limit`) mutuellement exclusifs et complets (ou aucun paramètre).
  - Remplacer `@Nonnull` par `@NotNull`, ajouter `@NotBlank` sur `question`/`answer`, et uniformiser `implements AddQuestionDto` (ou supprimer `sealed interface` si inutile).
  - Vérifier l'impact OpenAPI/frontend avant d'harmoniser les `@ResponseStatus(HttpStatus.CREATED)` / `NO_CONTENT` (ou conserver la compatibilité si déjà consommé par le client généré).
  - Gérer `IllegalArgumentException` sur `UUID.fromString` dans `me()` en retournant `401 Unauthorized`.
- **Critère d'acceptation testable** :
  - `GET /apiv1/themes?page=1` (sans `perPage`) ou `?page=1&perPage=10&offset=0&limit=5` retourne `400 Bad Request` avec le message de contrainte attendu.

---

### P-12 — `quezap-cli` : saisie mot de passe en clair, seeder actif en prod et incohérence de configuration
- **Sévérité** : Moyenne
- **Localisation** :
  - `back/quezap-cli/src/main/java/com/quezap/cli/UserCommands.java:98-186`
  - `back/quezap-cli/src/main/java/com/quezap/cli/SeedCommands.java:10-23`
  - `back/modules/application/src/main/java/com/quezap/application/seed/UserSeeder.java:24-34`
  - `back/quezap-cli/src/main/resources/application.yml:7`
- **Description** :
  - `UserCommands.readPassword` (`175-177`) appelle `lineReader.readLine(prompt, null)` : le paramètre de masquage `null` affiche le mot de passe saisi **en clair** sur le terminal, et un EOF/entrée nulle provoque un `NullPointerException` dans `new RawPassword(...)`.
  - `SeedCommands` (`seed users`) est enregistré sans `@Profile` : la commande est disponible même en environnement de production, et `UserSeeder` crée 10 utilisateurs (`user1..user10`) avec des mots de passe hardcodés `123Password.1..10` (de plus non idempotent).
  - `UserCommands.listUsers` (`105-114`) boucle sur `while (!pageUsers.isEmpty())` sans tester `paginatedUsers.hasNextPage()`, générant une requête SQL supplémentaire et accumulant tous les utilisateurs en mémoire.
  - `quezap-cli/src/main/resources/application.yml:7` définit `APP_DB_PASSWORD:quezap` alors que `quezap-api/src/main/resources/application.yml:16` définit `quezap_password`.
- **Correction proposée** :
  - Passer `'*'` (ou `(char) 0`) à `lineReader.readLine(prompt, '*')` et gérer le retour `null`.
  - Restreindre `SeedCommands` (et `UserSeeder`) avec `@Profile({"dev", "local", "seed"})`.
  - Arrêter la pagination de `listUsers` sur `!paginatedUsers.hasNextPage()` et afficher page par page.
  - Aligner la configuration par défaut DB entre CLI et API (ou l'externaliser).
- **Critère d'acceptation testable** :
  - Lors de l'exécution de `users add`, la saisie du mot de passe est masquée (`*`) et `Ctrl+D` renvoie un message d'erreur propre sans exception.
  - Avec le profil `prod`, la commande `seed users` n'est pas exposée dans le shell.

---

### P-13 — GraalVM RuntimeHints partiels/morts et garde-fous Gradle (NullAway / JaCoCo) non bloquants par défaut
- **Sévérité** : Moyenne
- **Localisation** :
  - `back/quezap-api/src/main/java/com/quezap/api/ApiApplication.java:24-41`
  - `back/quezap-api/src/main/java/com/quezap/api/v1/aot/QuezapApiRuntimeHints.java:45-99`
  - `back/modules/application/src/main/java/com/quezap/application/aot/QuezapRuntimeHints.java:168-175`
  - `back/modules/infrastructure/src/main/java/com/quezap/infrastructure/aot/QuezapRuntimeHints.java:250`
  - `back/build.gradle:70-89,112-120,301-304`
- **Description** :
  - `ApiApplication.ValidationRuntimeHints` (`36-41`) n'est pas incluse dans `@ImportRuntimeHints` (`24-28`, code mort).
  - `QuezapApiRuntimeHints` (`53-89`) liste manuellement 34 DTOs de session/image mais omet les DTOs `auth`, `users`, `themes`, `questions`, `quezaps`, `PageOfDto` et `ErrorResonseDto` (couverts indirectement par le scan de `Record.class` dans `application/aot/QuezapRuntimeHints`, mais fragile si un DTO devient une `class` ou en cas de sérialisation générique/exception).
  - `infrastructure/aot/QuezapRuntimeHints.java:250` n'enregistre les proxys JDK AOP que pour `DataSource` et `Repository`, sans `UsecaseExecutor` (`TransactionalUsecaseExecutor` annoté `@Transactional`) ni `SessionDataPort` (`SessionDataJpaAdapter`).
  - Dans `back/build.gradle`:
    - `enableNullAway` (`70-87`) est `false` par défaut sans `-Pnullaway=true` : `./gradlew check` / `test` laissent passer des violations de null-safety.
    - JaCoCo est configuré (`30, 114-117, 304`) mais aucune tâche `jacocoTestCoverageVerification` avec `violationRules` n'est définie ni rattachée à `check`.
- **Correction proposée** :
  - Supprimer `ApiApplication.ValidationRuntimeHints` (redondante avec le scan `ConstraintValidator.class`) et enregistrer dans `QuezapApiRuntimeHints` les DTOs non couverts ou un scan systématique des packages `com.quezap.api.v1.dto.**` et `com.quezap.api.errors.*`, ainsi que `UsecaseExecutor` si proxifié par JDK.
  - Valider l'image native (`nativeCompile` + smoke test HTTP/SSE/Jackson/Validation sur les endpoints auth/themes/questions/sessions) en CI.
  - Activer NullAway par défaut (avec opt-out explicite si besoin) et configurer `jacocoTestCoverageVerification` (seuils par module `domain`/`application`).
- **Critère d'acceptation testable** :
  - `./back/gradlew -p back check` exécute NullAway en erreur (`-Werror`), Spotless, Checkstyle et `jacocoTestCoverageVerification`.
  - Le binaire natif compilé démarre et répond correctement à `POST /apiv1/auth/login`, `GET /apiv1/themes`, `POST /apiv1/questions/*` et aux erreurs `400`/`404` JSON de `GlobalExceptionHandler`.
