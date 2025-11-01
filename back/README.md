# QueZap — Backend

Backend Spring Boot (Java 25) organisé en DDD et architecture hexagonale. Expose:
- une API HTTP (REST)
- un Shell CLI (Spring Shell)
- une compilation native (GraalVM)

[Documentation racine](../README.md)

## 🔧 Prérequis
- Java 25 (JDK)
- GraalVM 25 (optionnel, pour native-image)

## 🚀 Démarrage rapide
- Lancer l’API (profil par défaut):
```bash
./gradlew bootRun
```
- Lancer le Shell (profil cli):
```bash
SPRING_PROFILES_ACTIVE=cli ./gradlew bootRun --console=plain
```
- Développement local avec données initiales (profil local):
```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Profils disponibles: `default`, `cli`, `local`, `test`, `prod` (voir `src/main/resources/application-*.yml`).

## 🧪 Tests
```bash
./gradlew clean test -Pnullaway=true
```

Rapport JaCoCo: `build/reports/jacoco/test/html/index.html`.

## 📦 Build
- JAR exécutable:
```bash
./gradlew clean build -Pnullaway=true
```
- Binaire natif (GraalVM):
```bash
./gradlew clean nativeBuild -Pnullaway=true
```
- Native avec agent de traçage (hints réflexion):
```bash
./gradlew -Pagent nativeBuild
```

Exécuter le binaire natif:
```bash
./build/native/nativeCompile/quizz
```

Note: le support de `@Transactional` en native peut nécessiter des hints/ajustements.

## 🛠️ Troubleshooting (native / GraalVM)
Le projet intègre des hints AOT programmatiques et l’agent GraalVM pour faciliter la compilation native.

- Hints programmatiques
	- `@ImportRuntimeHints`: voir `QuizzApplication.ValidationRuntimeHints` (ex: `PaginationValidator`).
	- `com.quezap.aot.QuezapRuntimeHints` enregistre:
		- Ressources Apache Tika (MIME, parsers)
		- Réflexion pour `ConstraintValidator`, `Converter`, `JsonDeserializer`, `Record` et les Use Cases
		- Proxies JDK pour Spring AOP (ex: repositories, `DataSource`), afin que les proxys soient reconnus par native-image
	- Si une classe échoue en native avec une erreur de réflexion/proxy, ajoutez-la dans `QuezapRuntimeHints` avec les `MemberCategory` adaptés (constructeurs, méthodes, champs).

- Agent GraalVM (capture des usages au runtime)
	- Commande: 
		```bash
		./gradlew -Pagent nativeBuild
		```
	- L’agent génère des metadata fusionnées dans `src/main/resources/META-INF/native-image` (voir build.gradle: `graalvmNative.agent.metadataCopy`).
	- Le filtre `user-code-filter.json` limite la capture au code applicatif (réduit le bruit).

- Erreurs fréquentes et pistes
	- « Missing reflection config / ClassNotFoundException / NoSuchMethodException »: ajouter la classe dans `QuezapRuntimeHints#findAndRegisterForReflection` avec `INVOKE_DECLARED_CONSTRUCTORS` et/ou `INVOKE_DECLARED_METHODS`, `DECLARED_FIELDS` si nécessaire.
	- « Proxy non supporté » (AOP/transactionnel): s’assurer que l’interface ciblée est bien couverte par `registerJdkProxy` (ex: `Repository`, `DataSource`).
	- `@Transactional`: ces proxys reposent sur Spring AOP; en cas de souci, vérifier que le hint proxy est bien généré pour les interfaces concernées et que les use cases sont scannés.

## 🧭 Architecture (DDD / Hexagonale)
- Domain: agrégats et VOs (ex: `User`, `Question`, `Session`, `Credential`…), invariants métiers, événements de domaine.
- Application: ports/use cases, exécution transactionnelle (`UseCaseExecutor`) et publication d’événements après commit.
- Interfaces: REST (contrôleurs `interfaces/api/v1`) et CLI (Spring Shell).
- Infrastructure: implémentations techniques (ex: repositories in-memory, S3, événements Spring).

Flux type:
`Interface (REST/CLI)` → `UseCase` → `Repositories/Services (ports)` → `Domain` → événements publiés après commit.

## ⚙️ Configuration
Fichiers: `src/main/resources/application.yml` (+ variantes par profil).

- Sécurité: Spring Security, API stateless, Argon2 pour le hachage des mots de passe.
- Stockage d’images (S3-like): variables attendues (selon l’implémentation infra S3)
	- `pictures-s3.host`
	- `pictures-s3.port`
	- `pictures-s3.public-endpoint`
	- `pictures-s3.bucket-name`
	- `pictures-s3.access-key-id`
	- `pictures-s3.secret-access-key`

## 🧰 Tâches Gradle utiles
- Format/qualité: 
```bash
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew checkstyleMain checkstyleTest
```
- Tests + couverture: 
```bash
./gradlew test jacocoTestReport
```
- Build avec scan:
```bash
./gradlew build --scan
```

## 📚 API & CLI en bref
- REST: endpoints sous `apiv1/...` (voir contrôleurs dans `interfaces/api/v1`).
- CLI: 
	- lister les utilisateurs: `users list`
	- ajouter: `users add --name <nom> --login <identifiant>` (mot de passe demandé en TTY)
	- supprimer: `users delete --loginOrId <valeur>`

## 🤝 Contributions
- Respecter le formatage (Spotless) et le lint (Checkstyle).
- Lancer les tests avec `-Pnullaway=true`.
- Envoyer un build vert avant PR.

