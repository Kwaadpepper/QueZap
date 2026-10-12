# Research & Architecture Decisions: Diffusion et animation de quiz interactif en classe

**Feature**: Diffusion de questions et animation de session en classe (`specs/001-live-quiz-session`)  
**Date**: 2026-10-12

## 1. Objectifs de Recherche & Décisions d'Architecture

### D1 : Orchestration de la progression automatique et de la pause (Backend vs Frontend)
- **Question** : La temporisation automatique (mode auto) et la pause doivent-elles être gérées par un timer serveur (scheduler backend) ou pilotées par le client Présentateur (enseignant) ?
- **Analyse** :
  - Dans l'architecture actuelle de QueZap, le client enseignant (`SessionPresenter`) possède les droits hôte et contrôle le déroulement en envoyant des requêtes HTTP (`POST /sessions/{id}/start`, `POST /sessions/{id}/reveal`, `POST /sessions/{id}/next-question`), puis le backend diffuse les événements SSE à tous les abonnés.
  - Si le backend gérait un timer autonome par session sans contrôle actif, une déconnexion de l'enseignant ou un temps de parole spontané risquerait d'entraîner des sauts inattendus sans que l'enseignant ne puisse intervenir immédiatement.
  - En revanche, un contrôle hôte côté frontend avec synchronisation d'état au backend via un endpoint léger (`POST /sessions/{id}/pause` ou payload SSE) permet à l'enseignant d'avoir la maîtrise absolue du tempo.
- **Décision** :
  - **Pilotage de session hybride** : Le client `SessionPresenter` gère la machine à états de transition automatique (compte à rebours de question puis compte à rebours d'explication réglable, 15s par défaut).
  - Un endpoint backend `POST /sessions/{id}/pause` (avec payload `{ "paused": boolean }`) est exposé afin d'enregistrer l'état de pause et de le diffuser instantanément via SSE (`SESSION_PAUSED` / `SESSION_RESUMED`) à tous les élèves et au tableau.
  - Ainsi, si l'enseignant clique sur "Pause explications", le timer se fige immédiatement sur le présentateur et l'événement SSE gèle les timers des élèves tout en affichant le bandeau "En pause".

### D2 : Écran élève après vote & Anti-distraction
- **Question** : Comment épurer le flux élève pour éliminer toute distraction et orienter le regard vers le tableau tout en conservant la synchronisation ?
- **Analyse** :
  - Dans `quizz-runner.html`, l'état `lastReveal` affichait jusqu'alors une carte imposante avec `+points`, `🏆 Classement #rank`, et des messages de victoire/échec.
  - La clarification a tranché :
    1. Dès le clic/vote validé : affichage statique sobre rappelant l'intitulé de la question et l'option choisie par l'élève, avec un message bienveillant invitant à regarder le tableau.
    2. Phase de révélation : aucun classement intermédiaire affiché sur mobile. La correction détaillée n'apparaît sur mobile **que si l'enseignant a activé cette option** (désactivée par défaut).
    3. Si pause active : bandeau bien visible "En pause - Écoutez le professeur".
- **Décision** :
  - Refonte du composant `quizz-runner` et de `quizz-question-*` pour introduire un état d'attente sobre post-vote : `VOTE_SUBMITTED_WAITING_REVEAL`.
  - La bannière de pause réagit au signal `isSessionPaused` propagé par `ActiveSessionStore`.

### D3 : Restitution finale (Tableau sans scroll + Mobile inclusif avec moyenne)
- **Question** : Comment garantir que le tableau ne scroll jamais (Top 5) et comment calculer les métriques collectives de groupe (moyenne de classe) ?
- **Analyse** :
  - Actuellement, le backend calcule `PodiumDto` (top 3) et `FinalLeaderboardEntryDto` (tous les élèves).
  - L'écran `session-presenter.html` en stage `PODIUM` affichait une boucle `@for` sur l'ensemble de la classe avec scroll.
- **Décision** :
  - **Backend** : Enrichir `SessionEndedPayload` (et `FinalSessionReportDto`) pour inclure :
    - `averageScore`: moyenne des scores de la classe (arrondie à l'entier).
    - `globalSuccessRate`: pourcentage global de bonnes réponses de la classe.
    - `top5`: liste restreinte aux 5 premiers (`List<FinalLeaderboardEntryDto>`).
    - `finalLeaderboard`: liste complète pour les mobiles des élèves.
  - **Écran Tableau (`session-presenter`)** :
    - Remplacement du tableau scrollable par un layout fixe en hauteur (`h-screen`, `overflow-hidden` ou flexbox compacte).
    - Affichage d'un bloc "Collectif de la classe" (Moyenne générale, Taux de réussite du groupe).
    - Affichage du Top 5 sous forme de 5 cartes compactes ou marches adaptées, sans aucun scroll vertical.
  - **Écran Élève (`ended.html`)** :
    - Conserve la carte de résultat individuel (bienveillante).
    - Affiche la moyenne de la classe comme repère collectif de groupe.
    - Intègre une liste accordéon / déroulante stylisée du classement complet, permettant à chaque élève de voir où il se situe à son rythme sans stigmatisation publique.

### D4 : Conformité DDD, NullAway et GraalVM AOT
- **Contraintes** :
  - Dans `modules:domain`, `Session` intègre le champ `ProgressionMode` (enum `MANUAL`, `AUTOMATIC`) et `isPaused`.
  - Nouveaux DTOs dans `quezap-api` (`SessionPauseDto`, métriques enrichies de `SessionEndedPayload`) déclarés dans `QuezapRuntimeHints` pour GraalVM AOT.
  - NullAway (`@NullMarked`) strictement respecté.
  - Client Hey-API mis à jour côté Angular (`bun run codegen`).
