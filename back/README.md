# Backend

## Prérequis

- java 25
- graalvm 25 (native compilation)

## Shell

Pour lancer le shell en développement :

```bash
SPRING_PROFILES_ACTIVE=cli ./gradlew bootRun --console=plain
```

## Tests

Pour lancer les tests

```bash
./gradlew clean test -Pnullaway=true
```

## Build

Pour construire les artefacts (Fat Jar)

```bash
./gradlew clean build -Pnullaway=true
```

Pour construire les artefacts natifs (GraalVm)

```bash
./gradlew clean nativeCompile -Pnullaway=true
```
