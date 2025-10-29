# Backend

[../README.md](../README.md)

## Prérequis

- java 25
- graalvm 25 (native compilation)

## Shell

Pour lancer le shell en développement :

```bash
SPRING_PROFILES_ACTIVE=cli ./gradlew bootRun --console=plain
```

## Développement

Pour développer en local et initialiser avec des données aléatoires

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew clean bootRun
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
./gradlew clean nativeBuild -Pnullaway=true
```

Pour compiler avec l'agent qui produit des hints pour la reflexion
```bash
./gradlew -Pagent nativeBuild
```

**Note**: Pour l'instant compiler nativement avec GraalVm ne semble pas évident avec @Transactional.

## Run

Pour lancer directement avec gradle
```bash
./gradlew clean bootRun
```

Pour lancer le produit natif
```bash
./build/native/nativeCompile/quizz
```

