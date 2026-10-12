# Implementation Plan: Diffusion et animation de quiz interactif en classe

**Branch**: `001-live-quiz-session` | **Date**: 2026-10-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-live-quiz-session/spec.md`

## Summary

Cette fonctionnalité dote le projet QueZap d'un moteur d'animation de sessions en classe favorisant l'attention, l'émulation collective saine et l'inclusion pédagogique (RQTH) :
1. **Pilotage enseignant flexible** : choix entre mode Manuel et mode Automatique (avec temporisation de l'explication réglable, 15s par défaut), et bouton de **Pause explications** accessible en permanence pour figer le chrono et synchroniser un bandeau "En pause" sur tous les écrans.
2. **Interface élève épurée anti-distraction** : affichage de l'intitulé et du choix validé avec renvoi vers le tableau, sans aucun classement intermédiaire polluant, et correction masquée par défaut sur mobile (activable à la demande du professeur).
3. **Restitution finale valorisante & collective** : au tableau, affichage du **Top 5 compact sans scroll** et de la **moyenne de score de la classe** pour mesurer la performance de groupe ; sur mobile, consultation privée de la performance individuelle et du classement intégral pour que chaque élève se situe sans exclusion.

## Technical Context

**Language/Version**: Java 25 (OpenJDK / GraalVM 25) & TypeScript 5.8 / Node 22 (Bun runtime pour tests/builds front).  
**Primary Dependencies**: Spring Boot 3.5.7, Angular 21, `@ngrx/signals`, Tailwind CSS v4, PrimeNG v21, Hey-API Client, Zod v4.  
**Storage**: PostgreSQL 17 via Spring Data JPA / Hibernate (modèle `SessionJpaEntity`).  
**Testing**: JUnit 5, MockMvc, AssertJ, NullAway (`-Pnullaway=true`), Jest pour Angular.  
**Target Platform**: Serveur Linux / conteneurs Docker & Navigateurs web (Vidéoprojecteur grand écran + smartphones élèves).  
**Project Type**: Monorepo Web Application (Backend Hexagonal DDD multi-modules + Frontend Angular Standalone).  
**Performance Goals**: Latence de synchronisation SSE de la pause et de passage aux questions < 500 ms.  
**Constraints**: 
- Strict DDD Hexagonal pur dans `modules:domain` (zéro dépendance Spring/framework, validation via `Domain.checkDomain`).
- NullAway activé sans avertissement (`@NullMarked`).
- Aucun DTO artisanal en front (génération Hey-API exclusive).
- Zéro `any` en front, validation Zod systématique.
- Zéro contrainte de rétrocompatibilité : projet au niveau 0 du développement, refonte propre sans compromis de migration.
- Aucune régression sur le build natif GraalVM 25 (`QuezapRuntimeHints`).
**Scale/Scope**: Sessions de classe typiques de 10 à 60 élèves simultanés, jusqu'à 60 questions par session.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **I. Découpage Hexagonal Strict (Backend)** : Invariants validés dans l'agrégat `Session` pur (`modules:domain`), cas d'utilisation orchestrés dans `modules:application`, adaptateurs dans `modules:infrastructure` et endpoints dans `quezap-api`.
- [x] **II. Null Safety & Rigueur Typée** : Annotations JSpecify complètes (`@NullMarked`), compilation validée avec `-Pnullaway=true`, aucun type `any` côté front.
- [x] **III. Synchronisation Client API sans Duplication** : Mise à jour des contrats OpenAPI (`/apiv1/sessions/{id}/pause` et métriques de fin), régénération du client Hey-API (`bun run codegen`).
- [x] **IV. Composants & Réactivité Angular 21** : Standalone Components purs, Signal Store (`ActiveSessionStore`), Signal inputs/outputs, nettoyage RxJS via `takeUntilDestroyed`.
- [x] **V. Compilation Native GraalVM 25** : Déclaration des nouveaux DTOs de pause et de métriques dans `QuezapRuntimeHints`.

## Project Structure

### Documentation (this feature)

```text
specs/001-live-quiz-session/
├── spec.md              # Spécification fonctionnelle et clarifications
├── plan.md              # Ce plan technique d'implémentation
├── research.md          # Décisions d'architecture (Phase 0)
├── data-model.md        # Modèle de données & contrats d'événements SSE (Phase 1)
├── contracts/           # Contrats OpenAPI des nouveaux endpoints
│   └── session-api.json
├── quickstart.md        # Scénario de test pas à pas enseignant + élèves
├── checklists/          # Grille d'assurance qualité des exigences
│   └── requirements.md
└── tasks.md             # Tâches d'implémentation (généré par /speckit-tasks)
```

### Source Code (repository root)

```text
back/
├── modules/domain/src/main/java/com/quezap/domain/
│   ├── models/entities/Session.java                  # Ajout ProgressionMode, isPaused, pauseSession(), resumeSession()
│   └── events/sessions/
│       ├── SessionPaused.java                        # Événement domaine de pause
│       └── SessionResumed.java                       # Événement domaine de reprise
├── modules/application/src/main/java/com/quezap/application/
│   ├── ports/sessions/PauseSession.java              # Port Use Case de pause
│   └── usecases/sessions/PauseSessionHandler.java    # Handler Use Case
├── modules/infrastructure/src/main/java/com/quezap/infrastructure/
│   └── persistence/jpa/entities/SessionJpaEntity.java# Colonnes is_paused, progression_mode, explanation_duration
└── quezap-api/src/main/java/com/quezap/api/v1/
    ├── routes/sessions/SessionController.java        # Endpoints POST /pause et calcul des métriques de groupe dans SESSION_ENDED
    ├── dto/sessions/                                 # DTOs de pause et settings
    ├── services/SessionSseService.java               # Diffusion des événements de pause
    └── aot/QuezapRuntimeHints.java                   # Enregistrement pour GraalVM 25

front/src/app/
├── features/admin/sessions/pages/session-presenter/
│   ├── session-presenter.ts                          # Machine à états auto/pause, timer d'explications (15s par défaut), Top 5
│   └── session-presenter.html                        # Bandeau "En pause", Top 5 compact sans scroll, moyenne de classe
├── features/quizz/
│   ├── stores/active-session.ts                      # Signal isSessionPaused, gestion de l'état post-vote
│   ├── services/session-observer/session-observer-sse.ts # Traitement des événements SESSION_PAUSE_STATUS et SESSION_ENDED enrichis
│   ├── pages/quizz-runner/
│   │   ├── quizz-runner.ts                           # Verrouillage post-vote sobre, affichage de la pause
│   │   └── quizz-runner.html                         # Vue sobre : énoncé + choix validé + "👀 Regarde le tableau"
│   └── pages/ended/
│       ├── ended.ts                                  # Récupération du classement complet et de la moyenne de classe
│       └── ended.html                                # Carte personnelle bienveillante + moyenne collective + classement intégral
```

## Complexity Tracking

Aucune violation de la constitution n'est requise. L'implémentation réutilise l'architecture existante de QueZap (Domain Events, SSE, Signal Stores, Tailwind v4) sans sur-ingénierie.
