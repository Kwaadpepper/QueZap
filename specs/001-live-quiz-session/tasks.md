# Tasks: Diffusion et animation de quiz interactif en classe

**Feature**: Diffusion et animation de session en classe (`specs/001-live-quiz-session`)  
**Input**: [spec.md](spec.md), [plan.md](plan.md), [data-model.md](data-model.md), [research.md](research.md), [quickstart.md](quickstart.md)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Préparation des contrats d'API et alignement des dépendances

- [x] T001 [P] Valider le contrat OpenAPI dans `specs/001-live-quiz-session/contracts/session-api.json`
- [x] T002 [P] Vérifier la conformité de l'environnement Gradle (`./back/gradlew checkstyleMain`) et des linters front (`bun run lint`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Évolution du modèle de domaine DDD, persistance JPA et événements de diffusion

**⚠️ CRITICAL**: Prérequis indispensables avant le branchement des interfaces et des flux temps réel

- [x] T003 Étendre l'agrégat `Session` dans `back/modules/domain/src/main/java/com/quezap/domain/models/entities/Session.java` avec `progressionMode` (`ProgressionMode: MANUAL | AUTOMATIC`), `isPaused` (boolean), `pauseSession()`, `resumeSession()`, et invariants associés
- [x] T004 [P] Créer les événements de domaine `SessionPaused` et `SessionResumed` dans `back/modules/domain/src/main/java/com/quezap/domain/events/sessions/`
- [x] T005 [P] Ajouter les colonnes et mappings JPA (`is_paused`, `progression_mode`, `explanation_duration`) dans `back/modules/infrastructure/src/main/java/com/quezap/infrastructure/persistence/jpa/entities/SessionJpaEntity.java`
- [x] T006 Ajouter la méthode de mise à jour de pause dans `SessionDataPort` et son implémentation JPA dans `back/modules/infrastructure/`
- [x] T007 Ajouter le broadcast de `SESSION_PAUSE_STATUS` et enrichir `SESSION_ENDED` (métriques collectives : moyenne classe, taux de réussite, top 5) dans `back/quezap-api/src/main/java/com/quezap/api/v1/services/SessionSseService.java`
- [x] T008 [P] Enregistrer les nouveaux DTOs de pause et de métriques dans `back/quezap-api/src/main/java/com/quezap/api/v1/aot/QuezapRuntimeHints.java` pour GraalVM 25
- [x] T009 Exposer l'endpoint `POST /apiv1/sessions/{id}/pause` dans `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java`
- [x] T010 Régénérer le client API TypeScript dans `front/` via `bun run codegen`

**Checkpoint**: Socle backend DDD, persistance JPA, SSE et client TypeScript générés et prêts.

---

## Phase 3: User Story 1 - Contrôle de la diffusion par l'enseignant (Manuel vs Auto & Pause) 🎯 MVP

**Goal**: Permettre à l'enseignant de choisir entre transition manuelle et automatique, de régler la durée d'explication (15s par défaut), et de suspendre la transition à tout moment via "Pause explications" avec affichage "En pause" et gel du timer.

**Independent Test**: Démarrer une session sur `session-presenter`, basculer en mode automatique, laisser s'achever une question, constater le compte à rebours de 15s de l'explication, cliquer sur "Pause explications" pour figer le chrono et afficher "En pause", puis reprendre.

### Implementation for User Story 1
- [x] T011 [US1] Ajouter les signaux de configuration (`progressionMode: 'MANUAL' | 'AUTOMATIC'`, `explanationDurationSeconds: 15`, `isPaused: boolean`) dans `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts`
- [x] T012 [US1] Implémenter la machine à états de transition automatique (enchaînement vote → révélation avec timer d'explication réglable → question suivante) dans `session-presenter.ts`
- [x] T013 [US1] Implémenter la méthode `togglePause()` appelant l'API backend et figeant les timers dans `session-presenter.ts`
- [x] T014 [US1] Intégrer les contrôles dans l'interface présentateur (sélecteur Manuel/Auto, bouton proéminent "Pause explications", et bandeau très visible "En pause" au tableau) dans `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html`
- [x] T015 [US1] Ajouter les tests unitaires Jest pour la gestion du mode auto et de la pause dans `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.spec.ts`

**Checkpoint**: User Story 1 opérationnelle : l'enseignant pilote le flux manuel/auto et suspend le défilement sans friction.

---

## Phase 4: User Story 2 - Écran élève recentré sur l'écoute et le tableau (Anti-distraction & Pause)

**Goal**: Après le vote, afficher sur le smartphone uniquement la question et le choix sélectionné avec invitation à regarder le tableau, masquer les classements intermédiaires, et afficher le bandeau "En pause" synchronisé.

**Independent Test**: Rejoindre une session sur mobile, voter à une question. Vérifier que l'écran mobile verrouille les choix et affiche sobrement la réponse choisie sans animations de score ou de podium. Vérifier que la pause activée au tableau affiche immédiatement "En pause" sur le mobile.

### Implementation for User Story 2
- [x] T016 [US2] Ajouter la réception de l'événement SSE `SESSION_PAUSE_STATUS` dans `front/src/app/features/quizz/services/session-observer/session-observer-sse.ts`
- [x] T017 [US2] Mettre à jour `ActiveSessionStore` dans `front/src/app/features/quizz/stores/active-session.ts` pour exposer le signal `isSessionPaused` et conserver le choix soumis par l'élève (`mySubmittedChoice`)
- [x] T018 [US2] Supprimer l'affichage des scores et classements intermédiaires de la phase de révélation dans `front/src/app/features/quizz/pages/quizz-runner/quizz-runner.html`
- [x] T019 [US2] Refondre l'écran d'attente post-vote dans `quizz-runner.html` pour afficher sobrement le rappel de la question, la réponse sélectionnée par l'élève, et la consigne bienveillante "👀 Regarde le tableau pour les explications"
- [x] T020 [US2] Intégrer un bandeau d'alerte très visible "⏸️ En pause - Écoutez le professeur" sur l'écran élève réagissant à `isSessionPaused` dans `quizz-runner.html`
- [x] T021 [US2] Ajouter les tests unitaires Jest pour le store et le composant runner dans `front/src/app/features/quizz/pages/quizz-runner/quizz-runner.spec.ts`

**Checkpoint**: User Story 2 opérationnelle : les élèves sont focalisés sur le tableau et synchronisés avec l'état de pause.

---

## Phase 5: User Story 3 - Épilogue du quiz : Top 5 sans scroll au tableau et classement complet privé sur mobile

**Goal**: À la clôture du quiz, afficher au tableau le Top 5 dans un gabarit compact sans scroll avec les métriques collectives (moyenne de classe), et sur chaque mobile le classement intégral avec repère bienveillant par rapport à la moyenne.

**Independent Test**: Clôturer un quiz de test avec au moins 6 participants. Vérifier que le vidéoprojecteur affiche exactement le Top 5 et la moyenne de classe sans scrollbar. Vérifier que chaque élève accède sur sa page `/quizz/ended` à son bilan personnel et au tableau déroulant complet de la classe.

### Implementation for User Story 3
- [x] T022 [US3] Mettre à jour le calcul de clôture dans `SessionController.java` pour intégrer `averageScore`, `globalSuccessRate`, `top5` et `finalLeaderboard` dans le payload `SESSION_ENDED`
- [x] T023 [US3] Adapter la réception de `SESSION_ENDED` dans `front/src/app/features/quizz/services/session-observer/session-observer-sse.ts` et la persistance dans `ActiveSessionPersistence`
- [x] T024 [US3] Remplacer le tableau scrollable par un layout fixe sans scroll (Top 5 compact + carte métriques collectives de classe) dans `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html`
- [x] T025 [US3] Adapter les styles CSS du présentateur pour garantir un affichage 100% sans défilement vertical sur résolutions 720p/1080p dans `session-presenter.css`
- [x] T026 [US3] Enrichir la page de fin élève dans `front/src/app/features/quizz/pages/ended/ended.ts` pour exposer la moyenne de classe et la liste ordonnée complète de tous les participants
- [x] T027 [US3] Intégrer dans `front/src/app/features/quizz/pages/ended/ended.html` la restitution bienveillante : carte personnelle, repère collectif de la moyenne, et classement intégral consultable
- [x] T028 [US3] Ajouter les tests unitaires Jest pour la page `ended` dans `front/src/app/features/quizz/pages/ended/ended.spec.ts`

**Checkpoint**: User Story 3 opérationnelle : émulation collective réussie au tableau sans scroll, inclusion et autonomie sur mobile.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validation qualité globale, conformité constitutionnelle et vérification end-to-end

- [x] T029 Exécuter la suite de tests backend avec NullAway : `./back/gradlew -p back clean test -Pnullaway=true`
- [x] T030 [P] Appliquer le formatage Spotless et Checkstyle : `./back/gradlew -p back spotlessApply checkstyleMain`
- [x] T031 Exécuter les linters et tests frontend : `cd front && bun run lint && bun run test`
- [x] T032 Compiler le frontend en production (AOT) : `cd front && bun run build`
- [x] T033 Valider le scénario de bout en bout décrit dans `specs/001-live-quiz-session/quickstart.md` (1 écran hôte + 2 mobiles)

---

## Dependencies & Execution Order

```mermaid
flowchart TD
  subgraph Phase 1 & 2
    T001[T001: Contrats OpenAPI] --> T003[T003: Agrégat Session Domain]
    T003 --> T004[T004: Domain Events Pause]
    T003 --> T005[T005: SessionJpaEntity]
    T005 --> T006[T006: SessionDataPort JPA]
    T006 --> T007[T007: SSE Pause & Ended Metrics]
    T007 --> T008[T008: GraalVM RuntimeHints]
    T008 --> T009[T009: Controller POST /pause]
    T009 --> T010[T010: Bun run codegen API]
  end

  subgraph Phase 3 - US1 (Contrôle Enseignant)
    T010 --> T011[T011: Signaux Presenter]
    T011 --> T012[T012: Auto Progression Machine]
    T012 --> T013[T013: Toggle Pause Presenter]
    T013 --> T014[T014: UI Presenter Auto/Pause/Bandeau]
    T014 --> T015[T015: Tests Jest Presenter]
  end

  subgraph Phase 4 - US2 (Écran Élève Sobre)
    T010 --> T016[T016: SSE Observer Pause Event]
    T016 --> T017[T017: ActiveSessionStore Pause & Choice]
    T017 --> T018[T018: Masquage Classement Intermédiaire]
    T018 --> T019[T019: Vue Sobre Post-Vote]
    T019 --> T020[T020: Bandeau Pause Mobile]
    T020 --> T021[T021: Tests Jest Runner]
  end

  subgraph Phase 5 - US3 (Podium Top 5 & Moyenne)
    T010 --> T022[T022: Calcul Métriques Classe Backend]
    T022 --> T023[T023: Observer SSE Ended & Persistence]
    T023 --> T024[T024: Presenter Top 5 Sans Scroll]
    T024 --> T025[T025: CSS Layout No-Scroll 720p/1080p]
    T025 --> T026[T026: Ended Component Data]
    T026 --> T027[T027: Ended HTML Moyenne & Classement]
    T027 --> T028[T028: Tests Jest Ended]
  end

  subgraph Phase 6 - Polish
    T015 & T021 & T028 --> T029[T029: Backend Test NullAway]
    T029 --> T030[T030: Spotless & Checkstyle]
    T030 --> T031[T031: Frontend Lint & Jest]
    T031 --> T032[T032: Build AOT Production]
    T032 --> T033[T033: Quickstart Validation E2E]
  end
```

---

## Parallel Execution Opportunities

- **Backend JPA & Events** : T004 (`SessionPaused` / `SessionResumed`), T005 (`SessionJpaEntity`) et T008 (`QuezapRuntimeHints`) peuvent être implémentés en parallèle une fois l'agrégat T003 enrichi.
- **Frontend US1 vs US2** : Une fois le client Hey-API régénéré (T010), le développement du mode automatique enseignant (`session-presenter`) et de l'écran mobile épuré (`quizz-runner`) peut s'exécuter en parallèle sur des fichiers entièrement distincts.
- **Finitions (Phase 6)** : Les vérifications de style/linting backend (T030) et frontend (T031) peuvent être lancées en parallèle.
