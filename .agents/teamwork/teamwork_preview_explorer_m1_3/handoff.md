# Handoff Report — Explorer 3: Codebase Verification for Action Plan

**Date**: 2026-10-12  
**Author**: Explorer 3 (teamwork_preview_explorer_m1_3)  
**Target Milestone**: Milestone 1 / Milestone 3 Action Plan Authoring  
**Deliverable Scope**: Codebase verification of all 24 tasks from `.audit/BLACKBOARD.md` §4, assigned expert roles, strict testable acceptance criteria, and comprehensive blueprint for `.audit/ACTION_PLAN.md`.

---

## 1. Observation

All 24 tasks listed in `.audit/BLACKBOARD.md` §4 have been systematically cross-referenced against the actual files in `back/` and `front/`. Every single file cited exists at the exact specified path. Zero referenced files are hallucinated or fictional.

### Detailed Per-Task Codebase Findings

#### 🔴 Priorité P0 (Bloquants & Quick Wins — ~1-2 jours)

* **P0-1 : Icônes PrimeIcons manquantes (F1)**
  * **Observed Files**:
    * `front/package.json` : `grep -c primeicons front/package.json` = 0 (dépendance absente).
    * `front/angular.json` (l.54-56) et `front/src/styles/index.css` (l.1-4) : aucun import de `primeicons/primeicons.css`.
    * Templates impactés : `front/src/app/features/admin/dashboard/home/home.html` (l.34 `<i class="pi pi-plus">`, l.43, 63, 83 `<i class="pi pi-arrow-right">`), `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html` (l.24 `pi-expand`, l.100 `pi-arrow-right`, l.260 `pi-check` rond vert du reveal vide, l.284 `pi-arrow-right`), `session-report.html` (l.8, 18, 202, 204), `join-form.html` (l.58), `nickname-chooser.html` (l.10).
  * **Role**: UI/UX Designer (E2) & Lead Dev (E3).
  * **Testable Acceptance Criteria**:
    1. `grep -q '"primeicons"' front/package.json` renvoie le code 0.
    2. `grep -rn 'primeicons' front/src/styles/` renvoie l'import CSS.
    3. `cd front && bun run build` compile sans erreur.
    4. Sur `/admin/dashboard`, la carte « Créer un Quiz » affiche l'icône `+`, et sur l'écran reveal hôte (`session-presenter`), la pastille de bonne réponse contient la coche blanche `✓`.

* **P0-2 : URL de join réelle au lobby (F2)**
  * **Observed Files**:
    * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html` (l.33) : `<strong class="... underline">quezap.fr</strong>` en dur dans le texte d'instruction du lobby.
    * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l.110-112) : `joinUrl` calcule déjà `${origin}/quizz/join/${code}` mais aucune propriété n'est exposée pour le nom de domaine hôte affiché.
  * **Role**: CPO (E1) & UI/UX Designer (E2).
  * **Testable Acceptance Criteria**:
    1. `grep -rn 'quezap.fr' front/src/app/features/admin/sessions/pages/session-presenter/` retourne 0 occurrence.
    2. Sur le lobby hôte en cours d'exécution, le titre affiche dynamiquement l'URL locale/réelle (`window.location.host + '/quizz/join'`).

* **P0-3 : Rejet des réponses tardives post-reveal / hors timer (F5)**
  * **Observed Files**:
    * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (l.515-650) : `submitAnswer` vérifie l'état actif de la session, le token et le doublon, mais n'effectue aucun contrôle sur l'expiration du chrono (`Duration.between(slideStartedAt, Instant.now()) > timerSeconds`) ni sur le fait que la slide a déjà été révélée (`reveal`).
    * `SessionController.java` (l.653-664) : `reveal` émet `QUESTION_REVEAL` mais ne met à jour aucun état persistant de verrouillage de la slide.
    * `back/quezap-api/src/test/java/com/quezap/api/v1/routes/SessionControllerTest.java`.
  * **Role**: Lead Dev (E3).
  * **Testable Acceptance Criteria**:
    1. Un appel `POST /apiv1/sessions/{id}/answers` émis après l'appel à `POST /apiv1/sessions/{id}/reveal` ou après l'échéance `currentSlideStartedAt + timerSeconds` répond HTTP 400 Bad Request ou HTTP 409 Conflict avec un message explicite (« Le temps imparti est écoulé »).
    2. `./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.routes.SessionControllerTest"` passe avec succès.

* **P0-4 : Libellés Vrai / Faux sur l'hôte pour les questions BOOLEAN (F3)**
  * **Observed Files**:
    * `back/modules/domain/src/main/java/com/quezap/domain/models/entities/Question.java` (l.77-80) : `QuestionType.BOOLEAN` requiert exactement 1 réponse dans le domaine (`answers.size() == 1`).
    * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (l.388-390, 495-500) : sérialise les `safeAnswers` brutes de la question (donc 1 seule carte avec `label="True"`).
    * `front/src/app/features/quizz/components/question-view/boolean/boolean-question.ts` (l.49-52) : côté élève, `#defaultAnswers` génère 2 réponses : "Vrai" (0) et "Faux" (1).
    * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l.393-404) & `session-presenter.html` (l.228-266) : l'hôte itère sur `answers` et n'affiche qu'une seule carte « True ».
  * **Role**: Lead Dev (E3) & UI/UX Designer (E2).
  * **Testable Acceptance Criteria**:
    1. Pour toute question de type `BOOLEAN`, l'écran hôte (`session-presenter`) affiche exactement deux cartes de choix libellées « Vrai » et « Faux » en français (jamais une carte unique « True »).
    2. Lors du reveal, la bonne réponse (« Vrai » ou « Faux ») est surlignée avec le décompte et pourcentage de votes correspondant.

* **P0-5 : Calcul du delta / score / suppression du warning au reveal (F9)**
  * **Observed Files**:
    * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (l.1059) : `new LeaderboardEntryDto(rank++, p.getName(), p.getScore(), p.getScore(), 1)` injecte le score total cumulé dans le champ `delta`.
    * `front/src/app/features/quizz/services/session-observer/session-observer-sse.ts` (l.251, 261) : parse `l.previousScore` (champ inexistant dans le DTO backend `LeaderboardEntryDto(rank, participantName, score, delta, streak)`).
    * `front/src/app/features/quizz/pages/quizz-runner/quizz-runner.ts` (l.60-64) : `rank.score - rank.previousScore` donne `score - undefined = NaN`, entraînant `pointsEarned() = Math.max(0, NaN) = 0` permanent.
    * `front/src/app/features/quizz/components/question-view/parts/question-alert/question-alert.html` (l.1-13) : bandeau « Dépêchez-vous ! » reste affiché car le timer n'est pas masqué lors de l'affichage du modal de reveal.
  * **Role**: Lead Dev (E3) & UI/UX Designer (E2).
  * **Testable Acceptance Criteria**:
    1. L'événement SSE `INTERMEDIATE_LEADERBOARD` transporte le `delta` de points acquis sur la slide courante et/ou le `previousScore`.
    2. Sur l'interface élève mobile (`quizz-runner`), lors d'une bonne réponse, l'encadré « Résultats du tour » affiche « Points gagnés : +XXX pts » (valeur positive strictement supérieure à 0).
    3. Dès l'apparition des résultats du tour, l'alerte « Dépêchez-vous ! » est masquée.

* **P0-6 : Validation pseudo 2-50 caractères et feedback d'erreur visible (F8)**
  * **Observed Files**:
    * `front/src/app/features/quizz/components/nickname-chooser/nickname-chooser.ts` (l.47-50) : `nicknameForm` applique `minLength(3)` (incohérent avec le backend qui accepte dès 2 caractères) et **n'a aucun `maxLength(50)`**.
    * `front/src/app/features/quizz/components/nickname-chooser/nickname-chooser.html` (l.52-64) : l'élément `<input id="nickname-value">` n'a pas d'attribut `maxlength="50"`.
    * `nickname-chooser.ts` (l.115-128) : en cas d'erreur 400 ou 409 (pseudo déjà pris), le composant intercepte l'erreur sans la relier aux erreurs de champ visibles sous l'input.
  * **Role**: QA / Accessibilité (E4) & UI/UX Designer (E2).
  * **Testable Acceptance Criteria**:
    1. `<input id="nickname-value">` comporte `maxlength="50"`.
    2. La validation du formulaire accepte les pseudos de 2 à 50 caractères inclus.
    3. Si le backend renvoie 409 (doublon de pseudo), le message « Ce pseudo est déjà utilisé dans ce salon » s'affiche clairement sous le champ ou en toast explicite.
    4. `cd front && bun run test -- src/app/features/quizz/components/nickname-chooser/nickname-chooser.spec.ts` valide les règles 2-50 et le doublon.

* **P0-7 : Attribut `lang="fr"` et placeholders non rouges (F16, F19)**
  * **Observed Files**:
    * `front/src/index.html` (l.2) : `<html lang="en">` au lieu de `<html lang="fr">`.
    * `front/src/app/features/quizz/components/join-form/join-form.html` (l.19-27) : champ PIN affichant le placeholder `20082` qui hérite des styles d'erreur rouge `.ng-invalid` / `aria-invalid`.
    * `front/src/app/features/quizz/components/nickname-chooser/nickname-chooser.html` (l.55).
  * **Role**: QA / Accessibilité (E4) & UI/UX Designer (E2).
  * **Testable Acceptance Criteria**:
    1. `grep '<html lang="fr">' front/src/index.html` retourne 1 résultat.
    2. Les placeholders des formulaires de join et pseudo possèdent une classe neutre (ex: `placeholder:text-slate-400 dark:placeholder:text-slate-500`) et ne sont jamais teintés en rouge d'erreur avant toute saisie utilisateur.

* **P0-8 : Affichage du rang et score final élève sur `/quizz/ended` (F10)**
  * **Observed Files**:
    * `back/quezap-api/src/main/java/com/quezap/api/v1/dto/events/SessionEndedPayload.java` (l.6-7) : `public record SessionEndedPayload(..., List<FinalLeaderboardEntryDto> finalLeaderboard, PodiumDto podium)`.
    * `front/src/app/features/quizz/services/session-observer/session-observer-sse.ts` (l.274-278) : écoute `payload.leaderboard` au lieu de `payload.finalLeaderboard`. `payload.leaderboard` étant toujours `undefined`, `sessionPersistence.saveFinalResults(...)` n'est jamais exécuté.
    * `front/src/app/features/quizz/pages/ended/ended.html` (l.24-45) : `@if (res !== null)` n'est jamais rendu, l'élève ne voit que « Bravo, Léa ! » sans ses statistiques de fin.
  * **Role**: CPO (E1) & Lead Dev (E3).
  * **Testable Acceptance Criteria**:
    1. `session-observer-sse.ts` désérialise `payload.finalLeaderboard`.
    2. À la fin de la partie, l'écran élève `/quizz/ended` affiche les 3 cartes de synthèse : Classement final (`#1 sur 35`), Score total (`906 points`), et Réussite (`1 bonnes rép.`).
    3. `cd front && bun run test -- src/app/features/quizz/pages/ended/ended.spec.ts` confirme l'affichage complet des métriques.

---

#### 🟡 Priorité P1 (Évolutions Produit & Ergonomie — ~1-2 semaines)

* **P1-1 : Lancement de session en 1 clic depuis un Quezap**
  * **Observed Files**:
    * `front/src/app/features/admin/quezaps/components/quezap-card/quezap-card.ts` (l.43-55) : possède déjà la méthode `onQuezapLaunch()` créant la session via `sessionService.createSession()` et ouvrant `/projector/:id`.
    * `front/src/app/features/admin/dashboard/home/home.html` (l.172) : le bouton « Lancer une session » redirige vers le formulaire lourd à 3 étapes `/admin/sessions/create`.
  * **Role**: CPO (E1) & UI/UX Designer (E2).
  * **Testable Acceptance Criteria**:
    1. Depuis le Dashboard (`home.html`) et la liste des Quezaps (`quezap-card.html`), un bouton d'action directe permet de créer et projeter instantanément une session en moins de 1,5 seconde sans passer par le formulaire intermédiaire.

* **P1-2 : Timer serveur synchronisé (`deadline`) & Auto-reveal paramétrable**
  * **Observed Files**:
    * `back/quezap-api/src/main/java/com/quezap/api/v1/dto/events/QuestionStartPayload.java` (l.3-4) : transporte uniquement `timerSeconds: int`, sans horodatage absolu de fin.
    * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (l.369, 593) : dispose de `currentSlideStartedAt`.
    * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l.390, 560-580) : gère un simple `setInterval` local sans déclencher l'auto-reveal à 0s.
  * **Role**: Lead Dev (E3) & UI/UX Designer (E2).
  * **Testable Acceptance Criteria**:
    1. `QuestionStartPayload` inclut un champ `deadline: Instant` calculé côté serveur.
    2. Côté hôte (`session-presenter`), lorsque le compte à rebours atteint 0s, le reveal se déclenche automatiquement si l'option « Révélation automatique » est active.

* **P1-3 : Unification des symboles et couleurs de réponses Hôte / Mobile (F4)**
  * **Observed Files**:
    * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l.547-555) : utilise des caractères texte `▲, ◆, ●, ■` et classes `choice-red, choice-blue, choice-yellow, choice-green`.
    * `front/src/app/features/quizz/components/question-view/parts/question-icon/question-icon.ts` (l.17-26) : utilise des SVG pour `circle, cross, diamond, hexagon`.
  * **Role**: UI/UX Designer (E2) & QA / Accessibilité (E4).
  * **Testable Acceptance Criteria**:
    1. Les formes géométriques et couleurs associées aux 4 choix de réponse sont 100% identiques entre l'écran vidéoprojeté et l'interface mobile de l'élève (Cercle, Croix, Losange, Hexagone).

* **P1-4 : Restauration d'état complète de l'hôte au rechargement (F11)**
  * **Observed Files**:
    * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (l.986-1003) : `getInitialStreamData` ne renvoie que `LOBBY_STATE` et `QUESTION_START` générique sans le nombre de réponses déjà enregistrées (`answeredCount`) ni l'état de reveal.
    * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l.380-405) : réinitialise systématiquement `answersReceived` à 0.
  * **Role**: Lead Dev (E3).
  * **Testable Acceptance Criteria**:
    1. Si l'hôte recharge sa page en cours de question alors que 20 élèves ont voté, l'affichage indique immédiatement « 20 / 35 » et le temps restant effectif.
    2. Si la question est déjà révélée au rechargement, l'écran hôte se reconnecte directement en état `REVEAL`.

* **P1-5 : Écran de classement intermédiaire sur le vidéoprojecteur (F1)**
  * **Observed Files**:
    * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l.355, 368-450) : écoute `INTERMEDIATE_LEADERBOARD` mais ne le traite pas dans `handleEvent`.
    * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html` : aucun gabarit pour un stade `LEADERBOARD` entre deux questions.
  * **Role**: UI/UX Designer (E2) & CPO (E1).
  * **Testable Acceptance Criteria**:
    1. Entre le reveal et la question suivante, l'enseignant peut afficher un écran de classement intermédiaire projetant le Top 5 des élèves et leurs scores.

* **P1-6 : Sticky footer du lobby & exclusion accessible tactile/clavier (F17)**
  * **Observed Files**:
    * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html` (l.96-102) : bouton « Démarrer le Quizz » placé au-dessus de la grille des participants et repoussé hors du pli d'écran quand les participants sont nombreux.
    * `session-presenter.html` (l.113) : bouton d'exclusion configuré avec `opacity-0 group-hover:opacity-100` (inaccessible au clavier et sur écran tactile).
  * **Role**: UI/UX Designer (E2) & QA / Accessibilité (E4).
  * **Testable Acceptance Criteria**:
    1. Le bouton de démarrage de la session reste visible en barre fixe/sticky indépendamment du défilement des 35+ badges de participants.
    2. Le bouton d'exclusion de joueur est accessible au focus clavier (`group-focus-within:opacity-100`) et possède un libellé accessible `aria-label="Exclure le joueur {nom}"`.

* **P1-7 : Nettoyage du jargon technique sur le Dashboard enseignant (F18)**
  * **Observed Files**:
    * `front/src/app/features/admin/dashboard/home/home.html` (l.100-106) : cartes statiques « Mode Projection: 16:9 HD » et « Protocole Temps Réel: SSE Direct ».
  * **Role**: CPO (E1) & UI/UX Designer (E2).
  * **Testable Acceptance Criteria**:
    1. `grep -rn 'SSE Direct' front/src/app/features/admin/dashboard/` retourne 0 occurrence.
    2. Remplacé par des indicateurs orientés usage enseignant (ex: Total participants accueillis, Quiz animés).

* **P1-8 : Refonte ergonomique du Reveal mobile (F9)**
  * **Observed Files**:
    * `front/src/app/features/quizz/pages/quizz-runner/quizz-runner.html` (l.54-93) : encadré de reveal `fixed inset-x-4 bottom-6 z-50` superposé sur les cartes de réponses et le timer.
  * **Role**: UI/UX Designer (E2).
  * **Testable Acceptance Criteria**:
    1. L'écran de reveal mobile présente un statut non ambigu (Gagné / Perdu en vert ou rouge distinct), isole les cartes de réponse sans collision visuelle avec le timer, et affiche lisiblement l'explication pédagogique.

---

#### 🟢 Priorité P2 (Robustesse Technique, A11y & Architecture — ~2-3 semaines)

* **P2-1 : Accessibilité formulaire radio, aria-live & prefers-reduced-motion (F16)**
  * **Observed Files**:
    * `front/src/app/features/quizz/components/question-view/parts/question-answer/question-answer.html` (l.5-10) : `<input type="checkbox" [multiple]="!radio()">` au lieu d'un vrai input radio natif.
    * `front/src/styles/index.css` & `tailwind.css` : aucune règle `@media (prefers-reduced-motion: reduce)`.
    * `front/src/app/features/quizz/pages/quizz-runner/quizz-runner.html` : absence de conteneur `aria-live="polite"` pour les annonces d'état.
  * **Role**: QA / Accessibilité (E4).
  * **Testable Acceptance Criteria**:
    1. Pour les questions à choix unique, les réponses utilisent sémantiquement `role="radio"` ou `<input type="radio">` regroupés dans un `role="radiogroup"`.
    2. En activant `prefers-reduced-motion: reduce`, toutes les animations de rebond, de clignotement et les confettis sont désactivés.
    3. Les changements de statut (« Réponse envoyée », « Révélation ») sont annoncés par `aria-live="polite"`.

* **P2-2 : Sanitisation serveur des pseudos & protection injection CSV (F7)**
  * **Observed Files**:
    * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (l.700-710) : `exportCsv` concatène directement `p.getName()` sans échappement des caractères de formule (`=`, `+`, `-`, `@`) ni gestion des virgules/guillemets.
    * `SessionController.java` (l.270-310) : validation de pseudo n'interdisant pas les balises HTML `<>` stockées en base.
  * **Role**: Lead Dev (E3) & QA / Accessibilité (E4).
  * **Testable Acceptance Criteria**:
    1. L'API rejette les pseudos contenant des balises HTML avec HTTP 400 Bad Request.
    2. Tout pseudo débutant par `=`, `+`, `-`, `@` exporté dans le CSV est protégé par une apostrophe initiale `'` conformément aux règles OWASP CSV Injection.
    3. Test unitaire dans `SessionControllerTest.java` validant la neutralisation des formules.

* **P2-3 : SSE résilient (reconnexion hôte, rejeu Last-Event-ID, keep-alive > 30 min) (F14)**
  * **Observed Files**:
    * `back/quezap-api/src/main/java/com/quezap/api/v1/services/SessionSseService.java` (l.25, 41) : timeout fixe `DEFAULT_TIMEOUT_MS = 30 * 60 * 1000L` sans rejeu de séquence.
    * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l.333-366) : `eventSource` instancié sans gestionnaire `onerror`.
  * **Role**: Lead Dev (E3).
  * **Testable Acceptance Criteria**:
    1. En cas de coupure réseau transitoire sur l'hôte, l'EventSource se reconnecte automatiquement sans rafraîchissement manuel de la page.
    2. Le backend accepte l'en-tête `Last-Event-ID` et renvoie les événements manqués depuis le tampon en mémoire.

* **P2-4 : Conformité stricte Hey-API & Zod (suppression des DTOs artisanaux) (F15)**
  * **Observed Files**:
    * `front/src/app/features/admin/sessions/services/session.ts` (l.7) : `export interface SessionApiResponse`.
    * `front/src/app/features/admin/sessions/services/session-http.ts` (l.14) : `interface PageOfApiResponse<T>`.
    * `front/src/app/features/quizz/services/session-api/session-api-http.ts` (l.27) : `interface JoinSessionApiResponse`.
    * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l.25-80) : interfaces artisanales `SseEnvelope`, `SsePayload`, `SseQuestion`, `SseAnswer`.
    * `front/src/app/features/admin/themes/services/theme-http.ts` (l.62) : contournement `as unknown as { uuid?: string }`.
  * **Role**: Lead Dev (E3).
  * **Testable Acceptance Criteria**:
    1. `grep -rn 'interface SessionApiResponse' front/src/` retourne 0 occurrence.
    2. `grep -rn 'interface PageOfApiResponse' front/src/` retourne 0 occurrence.
    3. `grep -rn 'as unknown as' front/src/` retourne 0 occurrence.
    4. Tous les appels HTTP utilisent exclusivement les types générés par Hey-API dans `@quezap/core/api-client`.
    5. `cd front && bun run build && bun run lint` passe sans aucun avertissement.

* **P2-5 : Découpage hexagonal de `SessionController` & Runtime Hints AOT GraalVM (F13)**
  * **Observed Files**:
    * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (1186 lignes, injections directes des dépôts JPA `SessionJpaRepository`, `QuezapJpaRepository`, `UserJpaRepository`).
    * `back/modules/infrastructure/src/main/java/com/quezap/infrastructure/aot/QuezapRuntimeHints.java` (l.29-45).
  * **Role**: Lead Dev (E3).
  * **Testable Acceptance Criteria**:
    1. `SessionController.java` ne contient aucune importation directe de classes du package `com.quezap.infrastructure.adapter.persistence.jpa.repositories.*`.
    2. Les opérations métier sont déléguées à des handlers `@Usecase` situés dans `modules:application`.
    3. Les nouveaux DTOs d'événements SSE sont enregistrés dans `QuezapRuntimeHints`.
    4. `./back/gradlew -p back clean test -Pnullaway=true` réussit avec zéro violation.

* **P2-6 : Suite Playwright E2E automatisée et test de charge de classe**
  * **Observed Files**:
    * `.audit/sim2.cjs` (script Playwright autonome ayant validé 1 hôte + 3 élèves UI + 32 élèves API).
    * `.agents/skills/e2e-playwright/SKILL.md` (conventions de test de session multi-onglets).
  * **Role**: QA / Accessibilité (E4) & Lead Dev (E3).
  * **Testable Acceptance Criteria**:
    1. Un scénario E2E complet (1 écran hôte 1920x1080 + 3 élèves mobiles 390x844 + 32 bots concurrents) s'exécute via la commande `bun run test:e2e` en mode headless.
    2. Le test valide la création de session, l'entrée en lobby, le vote concurrent (35 participants en < 1s), le reveal et l'accès au podium sans échec.

* **P2-7 : Clarification produit de l'accès public `GET /apiv1/quezaps` (F20)**
  * **Observed Files**:
    * `back/quezap-api/src/main/java/com/quezap/api/v1/security/SecurityConfig.java` (l.60) : `auth.requestMatchers(HttpMethod.GET, "/apiv1/quezaps/**").permitAll()`.
    * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/quezaps/QuezapController.java` (l.168-170).
  * **Role**: CPO (E1) & Lead Dev (E3).
  * **Testable Acceptance Criteria**:
    1. Décision produit formellement arrêtée : maintien du catalogue public pour diffusion/templates ou verrouillage HTTP 401 pour utilisateurs non connectés.
    2. Test unitaire dans `SecurityConfigTest.java` validant la politique retenue.

* **P2-8 : Unification optionnelle des tokens de design (@theme CSS)**
  * **Observed Files**:
    * `front/src/styles/quezap-theme/colors.css` (l.1-48) : tokens `@theme` Tailwind.
    * `front/src/themes/Quezap.ts` (l.4-60) : preset Aura PrimeNG.
  * **Role**: UI/UX Designer (E2) & Lead Dev (E3).
  * **Testable Acceptance Criteria**:
    1. Les variables sémantiques de marque (`brand-green`, `brand-yellow`, `deepblue`) sont partagées harmonieusement entre Tailwind et PrimeNG sans duplication de codes hexadécimaux arbitraires.

---

## 2. Logic Chain

1. **Vérification de l'existence matérielle des fichiers** :
   * Chaque tâche a fait l'objet d'un examen via `view_file` ou `grep_search`.
   * Résultat : 100% des fichiers cibles existent réellement dans `back/` ou `front/`. Aucune entité n'a été inventée.
2. **Identification des causes profondes (Root Causes)** :
   * *Pourquoi Léa avait 0 point au reveal ?* `SessionController` (l.1059) envoie `delta = score`, le client SSE attend `previousScore`, `score - undefined` donne `NaN`, et `Math.max(0, NaN)` produit 0.
   * *Pourquoi l'écran de fin n'affichait ni rang ni score ?* `SessionEndedPayload.java` envoie `finalLeaderboard`, tandis que `session-observer-sse.ts` (l.278) lisait `payload.leaderboard` (clé inexistante).
   * *Pourquoi les icônes n'apparaissent pas ?* `primeicons` n'a jamais été ajouté à `front/package.json` ni importé dans le CSS racine.
   * *Pourquoi les daltoniens ne peuvent pas s'appuyer sur les formes ?* L'hôte utilise des symboles texte `▲, ◆, ●, ■` alors que les mobiles utilisent des icônes SVG Cercle, Croix, Losange, Hexagone.
3. **Attribution des rôles experts** :
   * Chaque tâche a été associée à l'expert dont le domaine d'expertise est prédominant :
     * E1 (CPO & stratégie commerciale) : P0-2, P0-8, P1-1, P1-5, P1-7, P2-7.
     * E2 (UI/UX & design system) : P0-1, P0-2, P0-4, P0-5, P0-6, P0-7, P1-1, P1-2, P1-3, P1-5, P1-6, P1-7, P1-8, P2-8.
     * E3 (Lead dev) : P0-1, P0-3, P0-4, P0-5, P0-8, P1-2, P1-4, P2-2, P2-3, P2-4, P2-5, P2-6, P2-7, P2-8.
     * E4 (QA & accessibilité) : P0-6, P0-7, P1-3, P1-6, P2-1, P2-2, P2-6.
4. **Formulation de critères d'acceptation stricts** :
   * Tous les critères d'acceptation formulés sont vérifiables par ligne de commande (`grep`, `bun run build`, `./back/gradlew test`), assertion d'API (codes de statut HTTP), ou inspection d'élément dans le DOM.

---

## 3. Caveats

1. **Aucune modification de code n'a été effectuée** : Conformément à la consigne stricte de lecture seule (R4) et d'hygiène git, aucun fichier dans `back/` ou `front/` n'a été modifié lors de cette exploration.
2. **Fichiers modifiés pré-existants** : Comme documenté dans `F21`, 9 fichiers de l'espace de travail étaient déjà modifiés avant le début de cet audit (`SessionController.java`, `SessionControllerTest.java`, `join-form.html/.ts`, `binary/boolean/quizz-question.html`, `question-icon.css`, `questions.css`). Ils ont été inspectés via `git diff` mais laissés intacts.
3. **Cas de test extrêmes à valider ultérieurement** : Les comportements en cas de coupure réseau brutale en mode avion et la navigation avec un vrai lecteur d'écran physique (NVDA, VoiceOver) restent à vérifier lors de la phase de validation QA (couvert dans les tâches P2-1 et P2-6).

---

## 4. Conclusion & Master Blueprint pour `.audit/ACTION_PLAN.md`

Toutes les informations nécessaires à la rédaction finale de `.audit/ACTION_PLAN.md` sont vérifiées, consolidées et prêtes.

### Architecture du Plan d'Action selon les 4 Axes Fondamentaux

```
.audit/ACTION_PLAN.md
├── 1. Vision Stratégique & Principes Directeurs
│   ├── Préservation des atouts majeurs (Gratuité, anonymat élève, identité QueZap « Jardin »)
│   ├── Résolution des frictions d'onboarding (Cible : < 30 secondes pour démarrer)
│   └── Alignement architectural strict sur AGENTS.md
│
├── 2. Matrice de Priorisation & Rôles
│   ├── Tableau synthétique des 24 tâches (Priorité, ID, Intitulé, Responsable principal, Effort)
│   └── Couverture croisée des 4 axes :
│       ├── Axe 1 : Commercial & Stratégie Produit (CPO / E1) -> P0-2, P0-8, P1-1, P1-5, P1-7, P2-7
│       ├── Axe 2 : Ergonomie & Design System (UI/UX / E2) -> P0-1, P0-2, P0-4, P0-6, P0-7, P1-3, P1-6, P1-8, P2-8
│       ├── Axe 3 : Architecture & Ingénierie (Lead Dev / E3) -> P0-3, P0-4, P0-5, P1-2, P1-4, P2-3, P2-4, P2-5
│       └── Axe 4 : Qualité, Robustesse & A11y (QA / E4) -> P0-6, P0-7, P1-3, P1-6, P2-1, P2-2, P2-6
│
├── 3. Fiches de Tâches Détaillées
│   ├── P0-1 à P0-8 (Quick Wins & Bloquants) : Description, Fichiers réels cibles, Responsable, Critères vérifiables
│   ├── P1-1 à P1-8 (Évolutions Produit & Confort) : Description, Fichiers réels cibles, Responsable, Critères vérifiables
│   └── P2-1 à P2-8 (Robustesse, A11y & Architecture) : Description, Fichiers réels cibles, Responsable, Critères vérifiables
│
└── 4. Protocole de Validation & Filet de Sécurité
    ├── Validation Backend (NullAway, Spotless, Checkstyle, Tests unitaires & OpenAPI)
    ├── Validation Frontend (AOT Angular build, ESLint, Stylelint, Tests Jest & Hey-API)
    └── Validation E2E Multi-Clients (Simulation classe Playwright 1 Hôte + N Mobiles)
```

---

## 5. Verification Method

Pour vérifier indépendamment les observations et conclusions de ce rapport :

1. **Vérification de l'absence de fichiers hallucines** :
   ```bash
   test -f front/package.json && \
   test -f front/src/index.html && \
   test -f front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts && \
   test -f front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html && \
   test -f front/src/app/features/quizz/services/session-observer/session-observer-sse.ts && \
   test -f back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java && \
   test -f back/quezap-api/src/main/java/com/quezap/api/v1/dto/events/SessionEndedPayload.java && \
   echo "ALL FILES VERIFIED OK"
   ```
2. **Vérification du mismatch de payload fin de partie (P0-8)** :
   * Observer `SessionEndedPayload.java` (l.7) : `finalLeaderboard`.
   * Observer `session-observer-sse.ts` (l.278) : `payload.leaderboard`.
3. **Vérification de l'absence de PrimeIcons (P0-1)** :
   * `grep -c primeicons front/package.json` renvoie `0`.
4. **Vérification de l'URL quezap.fr en dur (P0-2)** :
   * `grep -n 'quezap.fr' front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html` renvoie la ligne 33.
5. **Vérification de l'absence de modification de code lors de l'exploration** :
   * `git status --short` confirme qu'aucune modification supplémentaire n'a été apportée en dehors de `.agents/teamwork/`.
