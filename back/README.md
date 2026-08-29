# QueZap - Backend

Backend Spring Boot (Java 25) organisé en DDD et architecture hexagonale. Expose:
- une API HTTP (REST)
- un Shell CLI (Spring Shell)
- des binaires natifs (GraalVM)

[Documentation racine](../README.md)

## 🔧 Prérequis

- Java 25 (JDK) - la toolchain Gradle cible Java 25
- GraalVM 25 (optionnel, pour native-image)

## 🧭 Architecture (multi-modules)

- `modules:domain` - modèle métier (agrégats, VOs, invariants, événements)
- `modules:application` - ports/use cases, exécution transactionnelle, publication d’événements
- `modules:infrastructure` - implémentations techniques (S3, sécurité, persistance, événements)
- `modules:shared` - types partagés transverses
- Applications :
  - `quezap-api` - API REST Spring Boot
  - `quezap-cli` - CLI Spring Shell

## 🚀 Démarrage rapide

API (REST) :
```bash
./gradlew :quezap-api:bootRun
```
CLI (Shell) :
```bash
./gradlew :quezap-cli:bootRun --console=plain
```

Avec Docker Compose, le shell CLI est exposé via un service dédié et interactif :
```bash
docker compose --profile cli run --build --rm cli
```

Puis, dans le shell ouvert :
```bash
users add --name <nom> --email <email>
```
Le mot de passe est demandé en TTY.

Profils disponibles (API): `default`, `cli`, `test`, `prod` (voir `quezap-api/src/main/resources/application-*.yml`).
Exemple :
```bash
SPRING_PROFILES_ACTIVE=prod ./gradlew :quezap-api:bootRun
```

## 🧪 Tests

Exécuter l’ensemble des tests (tous modules) avec NullAway activé :
```bash
./gradlew clean test -Pnullaway=true
```

Rapports JaCoCo par module, ex. API: `quezap-api/build/reports/jacoco/test/html/index.html`.

## 📦 Build

Build complet (tous modules) :
```bash
./gradlew clean build -Pnullaway=true
```
JAR exécutable par application :
```bash
./gradlew :quezap-api:build
./gradlew :quezap-cli:build
```

### Binaire natif (GraalVM)

Compilation native par application :
```bash
./gradlew :quezap-api:nativeCompile
./gradlew :quezap-cli:nativeCompile
```
Exécuter les binaires générés :
```bash
./quezap-api/build/native/nativeCompile/quezap-api
./quezap-cli/build/native/nativeCompile/quezap-cli
```

Note : le support de certaines fonctionnalités (AOP/transactions) peut nécessiter des runtime hints.

## 🛠️ Native image - Hints & Agent

Le projet intègre des hints AOT programmatiques et l’agent GraalVM pour faciliter la compilation native.

- Hints programmatiques
  - `@ImportRuntimeHints`: voir `ApiApplication.ValidationRuntimeHints` (ex: `PaginationValidator`).
  - `com.quezap.application.aot.QuezapRuntimeHints` (application):
    - Réflexion pour `ConstraintValidator`, `Converter`, `JsonDeserializer`, `Record` et les Use Cases
  - `com.quezap.infrastructure.aot.QuezapRuntimeHints` (infrastructure):
    - Proxies JDK pour Spring AOP (ex: `Repository`, `DataSource`) afin que les proxys soient reconnus par native-image
  - Si une classe échoue en native (réflexion/proxy), ajoutez-la dans le hint approprié avec les `MemberCategory` adaptés.

- Agent GraalVM (capture au runtime)
  - L’agent est configuré en mode « conditional » et fusionne automatiquement la metadata générée pendant `test` dans `src/main/resources/META-INF/native-image` (voir `graalvmNative.agent.metadataCopy`).
  - Le filtre `user-code-filter.json` limite la capture au code applicatif.

## ⚙️ Configuration (API)

Fichiers : `quezap-api/src/main/resources/application.yml` (+ variantes par profil).

- Sécurité : Spring Security, API stateless
- Stockage d'images (S3-like) : variables attendues
  - `APP_S3_HOST`, `APP_S3_PORT`, `APP_S3_PUBLIC_ENDPOINT`
  - `APP_S3_BUCKET_NAME`, `APP_S3_ACCESS_KEY_ID`, `APP_S3_SECRET_ACCESS_KEY`
- JWT: `APP_JWT_SECRET_KEY`, `APP_JWT_ISSUER`

## 🧰 Tâches Gradle utiles

Format/qualité :
```bash
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew checkstyleMain checkstyleTest
```
Tests + couverture :
```bash
./gradlew test jacocoTestReport
```
Build avec scan :
```bash
./gradlew build --scan
```

## 📚 API & CLI en bref

- REST : endpoints sous `apiv1/...` (voir contrôleurs dans `quezap-api/src/main/java/com/quezap/interfaces/api/v1`).
- CLI :
  - lister les utilisateurs: `users list`
  - ajouter: `users add --name <nom> --email <email>` (mot de passe demandé en TTY)
  - supprimer: `users delete --loginOrId <valeur>`

## 🤝 Contributions

- Respecter le formatage (Spotless) et le lint (Checkstyle).
- Activer NullAway localement pour les CI-like: `-Pnullaway=true`.
- Pousser un build/test vert avant PR.
