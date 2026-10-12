# Plan d'Action Directeur (Master Action Plan) — QueZap

**Version** : 1.0.0 — Document de Référence Post-Audit  
**Date** : 12 Octobre 2026  
**Auteurs & Brigade d'Experts** :  
- **E1 — CPO & Stratégie Commerciale**  
- **E2 — UI/UX Designer & Design System Lead**  
- **E3 — Lead Software Engineer (Architecture Hexagonale & DDD)**  
- **E4 — Responsable QA & Spécialiste Accessibilité (RGAA / WCAG)**  
**Statut de l'Audit** : Lecture Seule Conforme (Aucune altération du code source dans `back/` ou `front/`).  
**Périmètre d'Application** : Monorepo QueZap (Spring Boot 3.5.7 / Java 25 / GraalVM 25 & Angular 21 / Tailwind CSS v4 / PrimeNG v21).

---

## 1. Résumé Exécutif & Vision Produit (Executive Summary)

### 1.1 Contexte et Objectif de l'Audit
QueZap est une solution web d'animation de quiz pédagogiques interactifs en temps réel, articulée autour de deux interfaces synchronisées via **Server-Sent Events (SSE)** :
1. **L'écran Enseignant / Hôte** (vidéoprojecteur, résolution cible 1920×1080), permettant la configuration, le lancement du lobby, la projection des questions et la célébration des résultats.
2. **L'interface Élève Mobile** (smartphone, résolution cible 390×844), permettant l'accès instantané via QR Code ou code PIN à 5 chiffres, le choix d'un pseudonyme, le vote interactif et la consultation des feedbacks de tour.

L'audit approfondi mené en lecture seule sur la base de 31 captures visuelles réelles, d'une simulation multi-clients (1 hôte + 35 participants concurrents via `.audit/sim2.cjs`) et d'une analyse rigoureuse du code source a permis d'isoler **21 faits objectifs majeurs (F1 à F21)**.

### 1.2 Principes Fondamentaux Inaltérables (Identity Preservation)
Toute action d'amélioration proposée dans ce document respecte scrupuleusement l'ADN fondateur de QueZap :
* **100% Gratuit et Sans Publicité** : Aucun modèle freemium déceptif, aucun bandeau publicitaire, aucune barrière de monétisation pour les fonctionnalités centrales en classe.
* **Anonymat et Zéro Compte pour les Élèves** : Les élèves ne créent aucun compte, ne saisissent aucun mot de passe et ne divulguent aucune donnée personnelle autre qu'un pseudonyme de session éphémère.
* **Souveraineté et Conformité RGPD Éducation** : Hébergement autonome des données, aucune dépendance vers des trackers tiers.
* **Identité Visuelle « Jardin & Verdure »** : Préservation des nuances végétales (`brand-green`, tons terre et floraux) et de l'univers illustré convivial qui différencient QueZap de l'ambiance agressive ou surchargée de Kahoot.

### 1.3 Synthèse du Diagnostic des 4 Experts
* **CPO (E1)** : Le produit possède d'immenses atouts pour conquérir le corps enseignant francophone (lancement rapide, export CSV des notes, podium dynamique). Cependant, des frictions critiques d'onboarding (nom de domaine erroné sur l'écran d'accueil, formulaire de lancement à 3 étapes au lieu d'un clic direct) et l'absence de score final sur le mobile brisent l'expérience utilisateur.
* **UI/UX Designer (E2)** : La lisibilité globale à 10 mètres est prometteuse grâce au PIN géant. Néanmoins, l'absence des icônes PrimeIcons détériore l'aspect fini de l'application, l'écran de reveal mobile présente des collisions d'éléments et les symboles de vote diffèrent entre l'écran et le smartphone.
* **Lead Dev (E3)** : La tenue de charge à 35 joueurs concurrents (30 réponses traitées en 166 ms) est excellente. En revanche, l'acceptation de réponses tardives post-reveal constitue une faille d'intégrité majeure. Par ailleurs, la dette architecturale accumulée dans `SessionController.java` (1186 lignes couplées à Spring Data JPA) et l'usage de DTOs artisanaux dans Angular contreviennent aux standards `AGENTS.md`.
* **QA & Accessibilité (E4)** : Le contraste général et la navigation méritent d'être salués, mais l'incohérence des formes géométriques pénalise directement les élèves daltoniens. L'absence d'annonces vocales dynamiques (`aria-live`), l'utilisation de cases à cocher pour des choix uniques et les placeholders teintés en rouge nécessitent une remise à niveau immédiate.

### 1.4 La Cible : L'Onboarding en Moins de 30 Secondes
Le plan d'action vise à garantir qu'un professeur se connectant sur son poste puisse lancer un quiz existant et accueillir ses 35 élèves prêts à voter en **moins de 30 secondes**, sans aucune hésitation ergonomique ni défaillance technique.

---

## 2. Synthèse des Priorités & Feuille de Route (Roadmap)

Le plan est structuré en **3 paliers d'exécution prioritaires** (24 chantiers au total), équilibrant quick wins visuels, fiabilité temps réel, accessibilité universelle et conformité architecturale.

### 2.1 Tableau Récapitulatif des 24 Chantiers

| Priorité | ID | Intitulé du Chantier | Rôle Référent | Axe Stratégique | Fait / Preuve | Effort Estimé |
| :---: | :---: | :--- | :---: | :---: | :---: | :---: |
| 🔴 **P0** | **P0-1** | Intégration de PrimeIcons & Restauration Visuelle | UI/UX (E2) | UI / UX | F1 / `host_10_admin_dashboard.png` | 2 heures |
| 🔴 **P0** | **P0-2** | Dynamisation de l'URL de Join sur le Lobby Hôte | CPO (E1) | Commercial | F2 / `host_20_lobby_empty.png` | 1 heure |
| 🔴 **P0** | **P0-3** | Rejet Strict des Réponses Tardives Post-Reveal | Lead Dev (E3) | Technique | F5 / `sim2.log` (LATE ANSWER) | 3 heures |
| 🔴 **P0** | **P0-4** | Prise en Charge Hôte des Questions BOOLEAN | Lead Dev (E3) | Technique / UX | F3 / `host_22_question_running.png` | 3 heures |
| 🔴 **P0** | **P0-5** | Calcul du Delta de Score & Masquage de l'Alerte | Lead Dev (E3) | Technique / UX | F9 / `player_07_reveal_result.png` | 3 heures |
| 🔴 **P0** | **P0-6** | Validation Pseudo 2-50 car. & Erreur Visible | QA (E4) | QA / UX | F8 / `player_03b_lobby_long_name.png` | 2 heures |
| 🔴 **P0** | **P0-7** | Attribut `lang="fr"` & Placeholders Neutres | QA (E4) | QA / A11y | F16, F19 / `host_00_landing.png` | 1 heure |
| 🔴 **P0** | **P0-8** | Restitution du Rang & Score Final (`/quizz/ended`) | CPO (E1) | Commercial / Dev | F10 / `player_10_end_of_game.png` | 3 heures |
| 🟡 **P1** | **P1-1** | Lancement de Session en 1 Clic (Mes Quezaps) | CPO (E1) | Commercial / UX | Diagnostic E1 / `home.html` | 1 jour |
| 🟡 **P1** | **P1-2** | Timer Serveur Synchronisé & Auto-Reveal | Lead Dev (E3) | Technique / UX | F12 / `session-presenter.ts` | 2 jours |
| 🟡 **P1** | **P1-3** | Unification des Symboles & Couleurs Hôte / Mobile | UI/UX (E2) | UI / A11y | F4 / `host_27_question_3.png` | 2 jours |
| 🟡 **P1** | **P1-4** | Restauration d'État Complète au Rechargement Hôte | Lead Dev (E3) | Technique | F11 / `host_24_question_after_host_reload.png` | 2 jours |
| 🟡 **P1** | **P1-5** | Écran de Classement Intermédiaire Vidéoprojeté | UI/UX (E2) | UI / Commercial | F1 / `session-presenter.ts` | 2 jours |
| 🟡 **P1** | **P1-6** | Sticky Footer Lobby & Exclusion Accessible | UI/UX (E2) | UX / A11y | F17 / `host_21_lobby_35_players.png` | 1 jour |
| 🟡 **P1** | **P1-7** | Épuration du Jargon sur le Dashboard Enseignant | CPO (E1) | Commercial / UX | F18 / `host_10_admin_dashboard.png` | 1 jour |
| 🟡 **P1** | **P1-8** | Refonte Ergonomique du Reveal Mobile Élève | UI/UX (E2) | UI / UX | F9 / `player_07_reveal_result.png` | 2 jours |
| 🟢 **P2** | **P2-1** | Accessibilité : Radio, `aria-live`, Reduced Motion | QA (E4) | QA / A11y | F16 / `question-answer.html` | 3 jours |
| 🟢 **P2** | **P2-2** | Sanitisation Pseudos & Protection Injection CSV | Lead Dev (E3) | Sécurité / QA | F7 / `sim2.log` (XSSNAME) | 2 jours |
| 🟢 **P2** | **P2-3** | Résilience SSE (Rejeu `Last-Event-ID`, Keep-Alive) | Lead Dev (E3) | Technique | F14 / `SessionSseService.java` | 3 jours |
| 🟢 **P2** | **P2-4** | Conformité Hey-API & Zod (Suppression DTOs Artisans) | Lead Dev (E3) | Technique | F15 / `session.ts`, `session-http.ts` | 2 jours |
| 🟢 **P2** | **P2-5** | Découpage Hexagonal `SessionController` & GraalVM | Lead Dev (E3) | Architecture | F13 / `SessionController.java` | 4 jours |
| 🟢 **P2** | **P2-6** | Suite E2E Playwright Automatisée de Simulation | QA (E4) | QA / Test | R1 / `.audit/sim2.cjs` | 3 jours |
| 🟢 **P2** | **P2-7** | Arbitrage Produit de l'Accès Public aux Quezaps | CPO (E1) | Commercial / Sec | F20 / `SecurityConfig.java` | 1 jour |
| 🟢 **P2** | **P2-8** | Harmonisation Transverse des Tokens de Design | UI/UX (E2) | Design System | Débat E2/E3 / `colors.css` | 2 jours |

### 2.2 Découpage Temporel & Jalons (Phasing)

```
Semaine 1 (J+1 à J+2) : JALON 1 — STABILISATION D'URGENCE (P0)
├── Déploiement des 8 correctifs P0 (icônes, URLs réelles, intégrité des scores et timers, rang final élève)
└── Validation : Zéro régression, compilation AOT front, NullAway back.

Semaine 2 à 3 (S+1 à S+2) : JALON 2 — VALEUR PRODUIT & ERGONOMIE SALLE (P1)
├── Implémentation du lancement en 1 clic, synchronisation fine du timer serveur, symboles daltoniens unifiés
└── Validation : Simulation 1 hôte + 35 élèves sans friction d'animation.

Semaine 4 à 5 (S+3 à S+4) : JALON 3 — ROBUSTESSE, SÉCURITÉ & ARCHITECTURE (P2)
├── Refactoring hexagonal de SessionController, sécurisation CSV OWASP, résilience SSE et conformité A11y
└── Validation : Suite Playwright E2E intégrée en CI, compilation native GraalVM 25.
```

---

## 3. Fiches Détaillées des 24 Chantiers d'Action

---

### 3.1 🔴 Priorité P0 : Bloquants & Quick Wins (~1 à 2 jours)

#### 📋 Fiche P0-1 : Intégration de PrimeIcons & Restauration Visuelle Globale
* **Axe** : UI/UX & Design System.
* **Rôle Assigné** : UI/UX Designer (E2) en collaboration avec Lead Dev (E3).
* **Constat & Preuves Visuelles** :
  * Fait **F1** : Le package `primeicons` est absent de `front/package.json` (`grep -c primeicons` = 0) et n'est importé nulle part dans `front/angular.json` ou `front/src/styles/index.css`.
  * Preuves visuelles : Carte « Créer un Quiz » sans icône de bouton (`host_10_admin_dashboard.png`), bouton « Démarrer le Quizz » et « Question suivante » sans flèche d'action (`host_21_lobby_35_players.png`, `host_25_reveal.png`), icône plein écran absente (`host_20_lobby_empty.png`), et pastille de bonne réponse vide au reveal sans coche blanche (`host_25_reveal.png`).
* **Fichiers Cibles Vérifiés** :
  * `front/package.json` (dépendance `primeicons`).
  * `front/src/styles/index.css` (l. 1-4).
  * Templates impactés : `front/src/app/features/admin/dashboard/home/home.html` (l. 34, 43, 63, 83), `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html` (l. 24, 100, 260, 284), `front/src/app/features/admin/sessions/pages/session-report/session-report.html` (l. 8, 18, 202, 204), `front/src/app/features/quizz/components/join-form/join-form.html` (l. 58), `front/src/app/features/quizz/components/nickname-chooser/nickname-chooser.html` (l. 10).
* **Spécification Technique & Préservation Produit** :
  * Ajouter la dépendance `primeicons` en version compatible avec PrimeNG v21 dans `front/package.json`.
  * Importer `@import 'primeicons/primeicons.css';` au sein de `front/src/styles/index.css`.
  * Vérifier le rendu des icônes vectorielles PrimeIcons sur toutes les plateformes sans déformer les polices de marque `Bangers` et `Lexend`.
* **Critères d'Acceptation Vérifiables** :
  1. `cd front && bun run test` et `bun run build` s'exécutent avec un code de retour 0.
  2. `grep -rn 'primeicons' front/src/styles/` retourne au moins 1 résultat d'import valide.
  3. La capture d'écran du dashboard hôte montre l'icône `+` sur le bouton de création de quiz, et l'écran de reveal affiche la coche blanche `✓` (`pi pi-check`) dans la pastille verte de réponse correcte.

---

#### 📋 Fiche P0-2 : Dynamisation de l'URL de Join et QR Code du Lobby Hôte
* **Axe** : Commercial / CPO & UI/UX.
* **Rôle Assigné** : CPO (E1) & UI/UX Designer (E2).
* **Constat & Preuves Visuelles** :
  * Fait **F2** : Le texte d'instruction vidéoprojeté aux élèves affiche en dur : `Rejoignez la partie sur quezap.fr` (`session-presenter.html` l. 33), alors qu'en environnement réel de déploiement (intranet d'établissement, domaine personnalisé, localhost ou IP locale), l'application n'est pas hébergée sur `quezap.fr`. De plus, la saisie brute du domaine sans chemin ne route pas directement vers le formulaire de code.
  * Preuve visuelle : `host_20_lobby_empty.png`, `host_21_lobby_35_players.png`.
* **Fichiers Cibles Vérifiés** :
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html` (l. 32-35).
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l. 108-115).
* **Spécification Technique & Préservation Produit** :
  * Dans `session-presenter.ts`, exposer un signal calculé `displayJoinUrl = computed(() => window.location.host + '/quizz/join')`.
  * Dans `session-presenter.html`, remplacer le texte figé par :  
    `Rejoignez la partie sur <strong class="...">{{ displayJoinUrl() }}</strong>`.
  * Maintenir la synchronisation avec le QR Code qui pointe déjà vers `window.location.origin + '/quizz/join/' + code`.
* **Critères d'Acceptation Vérifiables** :
  1. `grep -rn 'quezap.fr' front/src/app/features/admin/sessions/pages/session-presenter/` retourne 0 occurrence.
  2. Sur l'écran hôte démarré localement (`http://localhost:8000`), le titre affiche lisiblement `localhost:8000/quizz/join`.
  3. L'élève scannant le QR code ou saisissant l'URL affichée arrive directement sur le formulaire de join avec le PIN pré-renseigné.

---

#### 📋 Fiche P0-3 : Verrouillage Temporel & Rejet Strict des Réponses Tardives Post-Reveal
* **Axe** : Technique / Lead Dev & Intégrité Pédagogique.
* **Rôle Assigné** : Lead Software Engineer (E3).
* **Constat & Preuves Visuelles** :
  * Fait **F5** : Dans `sim2.log` (ligne `LATE ANSWER AFTER REVEAL 201 {"accepted":true}`), un appel `POST /apiv1/sessions/{id}/answers` émis après l'événement `QUESTION_REVEAL` ou après l'échéance du compte à rebours est accepté et crédite des points au joueur.
  * Analyse de code : `SessionController.java` (l. 515-650) vérifie la présence du token et l'absence de doublon, mais n'impose aucun garde-fou sur l'état de révélation ni sur la durée écoulée par rapport à `currentSlideStartedAt + timerSeconds`.
* **Fichiers Cibles Vérifiés** :
  * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (l. 520-655).
  * `back/quezap-api/src/test/java/com/quezap/api/v1/routes/SessionControllerTest.java`.
* **Spécification Technique & Préservation Produit** :
  * Dans `SessionController.submitAnswer`, ajouter une vérification d'invalidation :
    1. Si la slide a déjà été révélée (par exemple via un état de slide ou si un événement `QUESTION_REVEAL` a été émis pour cet index), lever `ResponseStatusException(HttpStatus.CONFLICT, "Les réponses sont verrouillées pour cette question")`.
    2. Si `Duration.between(slideStartedAt, Instant.now()).toSeconds() > timerSeconds + 2` (tolérance réseau de 2 secondes), lever `ResponseStatusException(HttpStatus.BAD_REQUEST, "Le temps imparti est écoulé")`.
  * Sauvegarder l'état révélé dans l'entité de session ou dans un registre d'état de slide.
* **Critères d'Acceptation Vérifiables** :
  1. Un test unitaire dans `SessionControllerTest.java` simule une soumission de réponse après révélation et valide la réception d'un statut HTTP 409 Conflict.
  2. Un test unitaire simule une soumission de réponse avec `Instant.now()` excédant le timer de plus de 3 secondes et valide la réception d'un statut HTTP 400 Bad Request.
  3. `./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.routes.SessionControllerTest"` s'exécute avec succès.

---

#### 📋 Fiche P0-4 : Prise en Charge Complète Hôte des Questions Booléennes (Vrai / Faux)
* **Axe** : Technique & UI/UX.
* **Rôle Assigné** : Lead Software Engineer (E3) & UI/UX Designer (E2).
* **Constat & Preuves Visuelles** :
  * Fait **F3** : Pour une question de type `BOOLEAN`, le modèle de domaine (`Question.java` l. 77-80) impose une unique réponse `answers.size() == 1` représentant la vérité (« True » ou « False »). Côté élève mobile, le composant `boolean-question.ts` (l. 49-52) synthétise deux boutons « Vrai » (index 0) et « Faux » (index 1). Mais sur l'écran hôte (`session-presenter.html` l. 228-266), l'application itère sur les réponses brutes renvoyées par l'API et affiche une **seule carte libellée en anglais « True »**.
  * Preuves visuelles : `host_22_question_running.png` (une seule carte affichée sur le vidéoprojecteur) vs `player_04_active_vote_boolean.png` (deux cartes sur smartphone).
* **Fichiers Cibles Vérifiés** :
  * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (l. 388-390, 495-500).
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l. 393-410).
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html` (l. 228-266).
* **Spécification Technique & Préservation Produit** :
  * Adapter `SessionController` ou le composant hôte `session-presenter.ts` pour que, lorsque le type de slide est `BOOLEAN`, la liste des réponses affichées sur l'écran hôte contienne systématiquement deux options françaises :
    * Choix 1 : « Vrai » (couleur verte/émeraude, index 0).
    * Choix 2 : « Faux » (couleur rouge/rose, index 1).
  * Lors du reveal, surligner la bonne réponse (« Vrai » si `answer.isCorrect == true`, « Faux » sinon) et répartir les statistiques de vote collectées entre ces deux options.
* **Critères d'Acceptation Vérifiables** :
  1. Sur une session contenant une question booléenne, l'écran vidéoprojeté `session-presenter` affiche 2 cartes distinctes « Vrai » et « Faux » (jamais une carte unique en anglais « True »).
  2. Les pourcentages de votes s'affichent correctement sur les deux cartes lors du reveal (`host_25_reveal.png`).
  3. `cd front && bun run test -- src/app/features/admin/sessions/pages/session-presenter/session-presenter.spec.ts` valide le formatage des choix booléens.

---

#### 📋 Fiche P0-5 : Calcul Cohérent du Delta de Score et Masquage de l'Alerte au Reveal
* **Axe** : Technique & UI/UX.
* **Rôle Assigné** : Lead Software Engineer (E3) & UI/UX Designer (E2).
* **Constat & Preuves Visuelles** :
  * Fait **F9** : Sur l'écran de résultat de tour élève (`player_07_reveal_result.png`), la carte affiche **« Points gagnés : 0 pts »** alors que le score total passe à 906 points (l'élève a correctement répondu). Cause : le backend injecte `p.getScore()` dans le paramètre `delta` (`SessionController.java` l. 1059 : `new LeaderboardEntryDto(rank++, p.getName(), p.getScore(), p.getScore(), 1)`), tandis que le front attend un champ `previousScore` inexistant dans le DTO (`session-observer-sse.ts` l. 251, 261), provoquant un calcul `score - undefined = NaN`, ramené à 0 par `Math.max(0, NaN)`.
  * De surcroît, le bandeau rouge d'avertissement **« Dépêchez-vous ! Il ne reste que 4s »** (`question-alert.html`) demeure affiché en arrière-plan du modal de reveal.
* **Fichiers Cibles Vérifiés** :
  * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (l. 1050-1065).
  * `front/src/app/features/quizz/services/session-observer/session-observer-sse.ts` (l. 245-265).
  * `front/src/app/features/quizz/pages/quizz-runner/quizz-runner.ts` (l. 60-65).
  * `front/src/app/features/quizz/components/question-view/parts/question-alert/question-alert.html` (l. 1-13).
* **Spécification Technique & Préservation Produit** :
  * Côté backend : calculer le delta réel de points obtenus par le participant sur la question courante (`pointsEarnedThisSlide = currentAnswer.getPointsAwarded()`) et le transmettre dans le champ `delta` du `LeaderboardEntryDto`.
  * Côté frontend : aligner le parsing SSE pour utiliser directement `l.delta` ou `l.score - (l.score - l.delta)`.
  * Dans `quizz-runner.ts` et `question-alert.html` : désactiver immédiatement `showTimerWarning` dès réception de `QUESTION_REVEAL`.
* **Critères d'Acceptation Vérifiables** :
  1. Lors d'une bonne réponse, l'encadré « Résultats du tour » sur smartphone affiche explicitement `Points gagnés : +XXX pts` avec une valeur positive strictement supérieure à 0.
  2. Le bandeau « Dépêchez-vous ! » disparaît dès l'affichage des résultats du tour.
  3. `./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.routes.SessionControllerTest"` passe avec succès.

---

#### 📋 Fiche P0-6 : Validation Stricte des Pseudonymes (2-50 car.) et Feedback d'Erreur Visuel
* **Axe** : QA & Accessibilité (E4) & UI/UX Designer (E2).
* **Rôle Assigné** : QA Specialist (E4).
* **Constat & Preuves Visuelles** :
  * Fait **F8** : Lorsqu'un élève saisit un pseudo de 52 caractères (`player_03b_lobby_long_name.png`), le bouton « Rejoindre le salon » reste désactivé sans aucune indication textuelle de l'erreur. De plus, `nickname-chooser.ts` (l. 49) impose un `minLength` de 3 caractères alors que le backend autorise dès 2 caractères, et ne définit aucun `maxLength(50)` sur le formulaire ni d'attribut `maxlength="50"` sur l'élément HTML `<input>`.
  * En cas d'erreur API 409 (pseudo déjà pris), l'erreur capturée par `catchError` (l. 115) affiche un toast générique sans expliciter le conflit de pseudo.
* **Fichiers Cibles Vérifiés** :
  * `front/src/app/features/quizz/components/nickname-chooser/nickname-chooser.ts` (l. 47-50, 115-128).
  * `front/src/app/features/quizz/components/nickname-chooser/nickname-chooser.html` (l. 52-64, 71-78).
  * `front/src/app/features/quizz/components/nickname-chooser/nickname-chooser.spec.ts`.
* **Spécification Technique & Préservation Produit** :
  * Aligner la validation du formulaire Angular :  
    `minLength(path.nicknameValue, 2, { message: 'Le pseudo doit faire au moins 2 caractères' })`  
    `maxLength(path.nicknameValue, 50, { message: 'Le pseudo ne peut pas dépasser 50 caractères' })`.
  * Ajouter `maxlength="50"` directement sur l'élément `<input id="nickname-value">`.
  * En cas d'erreur HTTP 409, afficher un message d'erreur rouge clair sous le champ : « Ce pseudo est déjà utilisé dans ce salon. Choisis un autre prénom ou ajoute une initiale. »
* **Critères d'Acceptation Vérifiables** :
  1. L'élément `<input id="nickname-value">` empêche la saisie de plus de 50 caractères dans le navigateur.
  2. Un pseudonyme de 2 caractères (ex: « Jo ») est considéré comme valide et active le bouton de validation.
  3. En simulant un doublon de pseudo existant, le message d'erreur d'unicité s'affiche en clair sous le champ de saisie.
  4. `cd front && bun run test -- src/app/features/quizz/components/nickname-chooser/nickname-chooser.spec.ts` valide les règles 2-50 et l'affichage d'erreur 409.

---

#### 📋 Fiche P0-7 : Internationalisation (`lang="fr"`) et Normalisation Neutre des Placeholders
* **Axe** : QA & Accessibilité (E4) & UI/UX (E2).
* **Rôle Assigné** : QA Specialist (E4).
* **Constat & Preuves Visuelles** :
  * Faits **F16** et **F19** :
    * `front/src/index.html` (l. 2) déclare `<html lang="en">` alors que l'intégralité de l'interface et du public cible scolaire est francophone. Cela fausse la prononciation des lecteurs d'écran (synthèse vocale anglaise sur du texte français).
    * Sur l'écran d'accueil (`host_00_landing.png`) et sur l'écran d'accès élève (`player_02_nickname_filled.png`), les placeholders (`20082` pour le code PIN, et le pseudo) héritent d'une teinte rouge vif issue des classes d'erreur de formulaire non touché (`quizzFieldError`, `.ng-invalid`), donnant l'illusion d'une erreur avant toute interaction.
* **Fichiers Cibles Vérifiés** :
  * `front/src/index.html` (l. 2).
  * `front/src/app/features/quizz/components/join-form/join-form.html` (l. 15-28).
  * `front/src/app/features/quizz/components/nickname-chooser/nickname-chooser.html` (l. 52-60).
* **Spécification Technique & Préservation Produit** :
  * Modifier `front/src/index.html` pour positionner `<html lang="fr">`.
  * Dans `join-form.html` et `nickname-chooser.html`, isoler le style des placeholders avec une classe CSS neutre explicite : `placeholder:text-slate-400 dark:placeholder:text-slate-500 font-normal`.
  * S'assurer que la directive `quizzFieldError` n'applique ses styles d'erreur rouge que lorsque le champ a été touché (`touched`) ou soumis (`submitted`).
* **Critères d'Acceptation Vérifiables** :
  1. `grep '<html lang="fr">' front/src/index.html` retourne 1 résultat.
  2. Sur la page d'accueil fraîchement chargée, le placeholder `20082` apparaît en gris ardoise neutre et non en rouge d'erreur.
  3. `cd front && bun run build` passe avec succès.

---

#### 📋 Fiche P0-8 : Restitution Complète du Bilan et Rang Final de l'Élève (`/quizz/ended`)
* **Axe** : Commercial / CPO (E1) & Lead Dev (E3).
* **Rôle Assigné** : CPO (E1) & Lead Dev (E3).
* **Constat & Preuves Visuelles** :
  * Fait **F10** : À la fin d'une session, l'écran élève `/quizz/ended` (`player_10_end_of_game.png`, `player_10b_end_of_game_p3.png`) affiche « Bravo, Léa ! » suivi d'un paragraphe générique et ré-affiche le formulaire de saisie de code PIN. **Aucun classement final, ni score total, ni nombre de bonnes réponses n'est visible**, brisant le moment fort de gratification de l'élève.
  * Cause identifiée : Dans `back/quezap-api/.../SessionEndedPayload.java` (l. 7), le champ est nommé `finalLeaderboard`, alors que `session-observer-sse.ts` (l. 278) écoute `payload.leaderboard`. `payload.leaderboard` étant indéfini, `sessionPersistence.saveFinalResults(...)` n'est jamais exécuté et `ended.html` n'affiche jamais le bloc `@if (res !== null)` (l. 24-45).
* **Fichiers Cibles Vérifiés** :
  * `back/quezap-api/src/main/java/com/quezap/api/v1/dto/events/SessionEndedPayload.java` (l. 6-7).
  * `front/src/app/features/quizz/services/session-observer/session-observer-sse.ts` (l. 272-292).
  * `front/src/app/features/quizz/pages/ended/ended.html` (l. 24-45).
  * `front/src/app/features/quizz/pages/ended/ended.spec.ts`.
* **Spécification Technique & Préservation Produit** :
  * Corriger la désérialisation dans `session-observer-sse.ts` : lire `payload.finalLeaderboard || payload.leaderboard`.
  * Sauvegarder les résultats personnels de l'élève (`rank`, `score`, `correctAnswers`, `totalParticipants`, `isPodium`) dans la persistance de session locale.
  * Mettre en valeur les 3 cartes récapitulatives dans `ended.html` (Classement, Score total, Bonnes réponses) avec les badges de podium 🥇🥈🥉 si l'élève est dans le top 3.
* **Critères d'Acceptation Vérifiables** :
  1. À l'issue d'une session terminée, l'écran élève `/quizz/ended` affiche les 3 cartes statistiques : Classement (ex: `#1 sur 35`), Score (ex: `906 points`), et Réussite (ex: `1 bonnes rép.`).
  2. Si l'élève termine sur le podium, le badge animé avec médaille d'or/argent/bronze s'affiche.
  3. `cd front && bun run test -- src/app/features/quizz/pages/ended/ended.spec.ts` confirme l'affichage complet des métriques de fin.

---

### 3.2 🟡 Priorité P1 : Évolutions Produit & Ergonomie (~1 à 2 semaines)

#### 📋 Fiche P1-1 : Lancement Immédiat en 1 Clic depuis le Dashboard et les Cartes Quezap
* **Axe** : Commercial / CPO & UI/UX.
* **Rôle Assigné** : CPO (E1) & UI/UX Designer (E2).
* **Constat & Preuves Visuelles** :
  * Diagnostic E1 : Actuellement, pour animer un quiz existant depuis le tableau de bord, l'enseignant doit cliquer sur « Lancer une session » (`home.html` l. 172), ce qui le redirige vers `/admin/sessions/create` (`host_12_admin_sessions_create.png`). Il doit alors franchir 3 étapes manuelles (choix du quiz, nommage de la session, validation), ce qui prend plus de 45 secondes.
  * Paradoxe de code : Le composant `quezap-card.ts` (l. 40-55) contient déjà une méthode `onQuezapLaunch()` prête à l'emploi qui génère automatiquement un nom de session horodaté et ouvre directement le projecteur en 1 clic.
* **Fichiers Cibles Vérifiés** :
  * `front/src/app/features/admin/dashboard/home/home.html` (l. 170-175).
  * `front/src/app/features/admin/dashboard/home/home.ts`.
  * `front/src/app/features/admin/quezaps/components/quezap-card/quezap-card.ts` (l. 43-55).
  * `front/src/app/features/admin/quezaps/components/quezap-card/quezap-card.html`.
* **Spécification Technique & Préservation Produit** :
  * Sur chaque carte de quiz du dashboard et de la page « Mes Quezaps », ajouter un bouton primaire d'action rapide : **« Lancer en direct ▶ »**.
  * Au clic, appeler `sessionService.createSession(quizId, quizTitle + ' - ' + heure)` puis naviguer immédiatement vers l'écran projecteur `/projector/:id`.
  * L'onboarding enseignant passe ainsi sous la barre des 15 secondes.
* **Critères d'Acceptation Vérifiables** :
  1. Depuis la page d'accueil admin (`/admin/dashboard`), un clic sur « Lancer » crée la session et affiche le lobby vidéoprojeté en moins de 1,5 seconde.
  2. Le formulaire complet `/admin/sessions/create` reste accessible via un bouton secondaire pour les besoins de paramétrage avancé.

---

#### 📋 Fiche P1-2 : Synchronisation Temporelle Hôte/Élève (`deadline`) & Révélation Automatique Paramétrable
* **Axe** : Technique & Ergonomie de Salle.
* **Rôle Assigné** : Lead Software Engineer (E3) & UI/UX Designer (E2).
* **Constat & Preuves Visuelles** :
  * Fait **F12** : Lorsque le compte à rebours de question atteint 0 seconde sur l'écran hôte (`session-presenter.ts` l. 295-302), le timer s'arrête simplement. L'enseignant doit manuellement cliquer sur « Révéler les réponses » pour débloquer la suite. Si l'enseignant est distrait, la classe reste bloquée.
  * Fait **F11** : L'événement `QUESTION_START` n'envoie qu'un nombre de secondes (`timerSeconds: int`, `QuestionStartPayload.java` l. 4) sans horodatage absolu, rendant impossible la resynchronisation en cas de latence réseau.
* **Fichiers Cibles Vérifiés** :
  * `back/quezap-api/src/main/java/com/quezap/api/v1/dto/events/QuestionStartPayload.java` (l. 3-4).
  * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (l. 365-375, 590-605).
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l. 290-310, 385-395).
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html`.
* **Spécification Technique & Préservation Produit** :
  * Enrichir `QuestionStartPayload` avec `Instant deadline` (calculé côté serveur : `Instant.now().plusSeconds(timerSeconds)`).
  * Sur l'écran hôte, ajouter une case à cocher dans les paramètres de session : « Révélation automatique à 0s » (activée par défaut).
  * Lorsque le compte à rebours atteint 0s, si l'option est active, déclencher automatiquement l'appel à `POST /sessions/{id}/reveal`.
* **Critères d'Acceptation Vérifiables** :
  1. `QuestionStartPayload` contient le champ `deadline`.
  2. En mode auto-reveal, dès que le timer atteint 0s, l'écran bascule automatiquement en état `REVEAL` sans intervention de l'enseignant.
  3. L'enseignant peut décocher l'option pour animer à son propre rythme.

---

#### 📋 Fiche P1-3 : Unification Stricte des Symboles et Couleurs Hôte / Élève
* **Axe** : UI/UX & Accessibilité (E2, E4).
* **Rôle Assigné** : UI/UX Designer (E2) & QA Specialist (E4).
* **Constat & Preuves Visuelles** :
  * Fait **F4** : Discordance complète entre l'écran hôte et le mobile :
    * Hôte (`session-presenter.ts` l. 547-555) : symboles textuels unicode `▲, ◆, ●, ■` et classes `choice-red, choice-blue, choice-yellow, choice-green`.
    * Mobile (`question-icon.ts` l. 17-26, `player_08_question_3.png`) : icônes vectorielles SVG (Cercle, Croix, Losange, Hexagone).
  * Un élève daltonien regardant le tableau pour identifier la forme de la bonne réponse ne retrouve pas la même géométrie sur son écran tactile.
* **Fichiers Cibles Vérifiés** :
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l. 547-555).
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html` (l. 230-265).
  * `front/src/app/features/quizz/components/question-view/parts/question-icon/question-icon.ts`.
* **Spécification Technique & Préservation Produit** :
  * Harmoniser les formes et couleurs selon la norme mobile établie :
    * Choix 1 (Haut Gauche) : **Cercle** (Fond Rouge Corail).
    * Choix 2 (Haut Droite) : **Losange** (Fond Bleu Roi).
    * Choix 3 (Bas Gauche) : **Croix** (Fond Jaune Ambré).
    * Choix 4 (Bas Droite) : **Hexagone** (Fond Vert Émeraude).
  * Réutiliser le composant SVG ou la police vectorielle de formes sur les cartes de réponse de l'écran hôte (`session-presenter.html`).
* **Critères d'Acceptation Vérifiables** :
  1. Comparaison visuelle entre `session-presenter` et `quizz-runner` : 100% de concordance des formes géométriques et des couleurs pour les 4 choix de réponse.
  2. Chaque carte comporte à la fois la forme, la lettre/chiffre d'ordre (1 à 4) et le texte de réponse pour une accessibilité maximale.

---

#### 📋 Fiche P1-4 : Restauration d'État Résiliente de l'Écran Hôte lors d'un Rechargement
* **Axe** : Technique / Lead Dev.
* **Rôle Assigné** : Lead Software Engineer (E3).
* **Constat & Preuves Visuelles** :
  * Fait **F11** : Si l'hôte recharge accidentellement sa page (touche F5) en cours de question alors que 31 élèves ont déjà répondu (`host_24_question_after_host_reload.png`), le compteur de réponses retombe à **« 0 / 35 réponses »** et le compte à rebours repart de la durée initiale complète (18s au lieu de 4s restantes).
  * Analyse : L'endpoint SSE `/events` (`SessionController.java` l. 986-1003) envoie `getInitialStreamData` qui ne contient ni `answersReceivedCount`, ni le temps écoulé réel, ni l'état de reveal.
* **Fichiers Cibles Vérifiés** :
  * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (l. 980-1005).
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l. 380-410).
* **Spécification Technique & Préservation Produit** :
  * Dans `InitialEventsData`, enrichir `startPayload` avec le décompte des réponses déjà reçues (`answeredCount`) et le temps restant effectif (`remainingSeconds = Math.max(0, timerSeconds - elapsedSeconds)`).
  * Si la question courante est déjà en état révélé, émettre immédiatement un événement `QUESTION_REVEAL` initial lors de la souscription SSE.
  * Dans `session-presenter.ts`, initialiser `answersReceived` avec la valeur reçue plutôt que 0.
* **Critères d'Acceptation Vérifiables** :
  1. Lors du rechargement de la page hôte après soumission de 20 réponses, le compteur affiche instantanément « 20 / 35 réponses ».
  2. Le temps restant affiché reprend exactement là où il en était (pas de réinitialisation à la durée maximale).
  3. Si la question était déjà révélée avant rechargement, l'hôte réapparaît directement en vue de révélation.

---

#### 📋 Fiche P1-5 : Introduction de l'Écran de Classement Intermédiaire Vidéoprojeté
* **Axe** : UI/UX & Stratégie Commerciale (E1, E2).
* **Rôle Assigné** : UI/UX Designer (E2) & CPO (E1).
* **Constat & Preuves Visuelles** :
  * Fait **F1** (Blackboard §1.1) : L'événement `INTERMEDIATE_LEADERBOARD` est diffusé par le backend (`SessionController.java` l. 1064) et écouté par l'hôte (`session-presenter.ts` l. 355), mais **n'est pas pris en compte dans le `switch (envelope.type)`**. Il n'existe aucun template HTML pour projeter le classement intermédiaire entre deux questions.
  * Les élèves voient leur rang individuel sur smartphone, mais la classe entière n'a pas de moment de célébration collective au tableau entre les questions.
* **Fichiers Cibles Vérifiés** :
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l. 355, 385-450).
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html` (après la vue de reveal).
* **Spécification Technique & Préservation Produit** :
  * Créer un stade d'affichage `LEADERBOARD` dans le cycle de vie de la session hôte : `LOBBY -> QUESTION -> REVEAL -> LEADERBOARD -> QUESTION_NEXT`.
  * Intégrer un gabarit de classement intermédiaire affichant le Top 5 des participants avec animation d'évolution de rang (flèche verte de progression `▲ +2`) et leur score cumulé.
  * Ajouter un bouton clair « Question suivante → » sur cet écran de classement.
* **Critères d'Acceptation Vérifiables** :
  1. Après la révélation d'une question, l'enseignant peut afficher le Top 5 de la classe en plein écran sur le vidéoprojecteur.
  2. Les scores et positions correspondent fidèlement aux résultats des calculs serveur.
  3. La transition vers la question suivante s'effectue sans rechargement.

---

#### 📋 Fiche P1-6 : Amélioration Ergonomique du Lobby Hôte (Bouton Sticky & Exclusion Accessible)
* **Axe** : UI/UX & Accessibilité (E2, E4).
* **Rôle Assigné** : UI/UX Designer (E2) & QA Specialist (E4).
* **Constat & Preuves Visuelles** :
  * Fait **F17** :
    * Sur l'écran hôte avec 35 participants (`host_21_lobby_35_players.png`), la grille des badges de joueurs pousse le contenu vers le bas. Sur un écran de résolution inférieure ou avec zoom, le bouton « Démarrer le Quizz » peut être repoussé hors de la zone visible.
    * Le bouton de suppression d'un joueur perturbateur (`session-presenter.html` l. 113) utilise `opacity-0 group-hover:opacity-100` : il est **inutilisable au clavier et sur écran tactile / TBI** (Tableau Blanc Interactif, sans souris).
* **Fichiers Cibles Vérifiés** :
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html` (l. 95-125).
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts`.
* **Spécification Technique & Préservation Produit** :
  * Fixer la barre d'action du lobby en pied de page sticky (`sticky bottom-0 bg-slate-900/90 backdrop-blur-md`) avec le compteur en temps réel de joueurs connectés et le bouton « Démarrer le Quizz ».
  * Sur les badges de joueurs, rendre le bouton d'exclusion visible au focus :  
    `opacity-0 group-hover:opacity-100 group-focus-within:opacity-100 focus:opacity-100`.
  * Ajouter un libellé accessible explicite `aria-label="Exclure le joueur {p.name}"`.
* **Critères d'Acceptation Vérifiables** :
  1. Avec 35 joueurs connectés, le bouton « Démarrer » reste accessible immédiatement sans avoir besoin de faire défiler la page.
  2. La navigation au clavier (Touche Tab) permet d'atteindre et d'activer le bouton d'exclusion de n'importe quel joueur.
  3. L'interaction tactile sur TBI permet de supprimer un joueur via un menu contextuel ou appui long.

---

#### 📋 Fiche P1-7 : Épuration du Jargon Technique sur le Tableau de Bord Enseignant
* **Axe** : Commercial / CPO & UI/UX.
* **Rôle Assigné** : CPO (E1) & UI/UX Designer (E2).
* **Constat & Preuves Visuelles** :
  * Fait **F18** : Sur le tableau de bord admin (`host_10_admin_dashboard.png`), deux des quatre cartes de statistiques mettent en avant des données d'infrastructure purement techniques sans pertinence pédagogique : « MODE PROJECTION 16:9 HD » et « PROTOCOLE TEMPS RÉEL SSE Direct » (`home.html` l. 100-106).
* **Fichiers Cibles Vérifiés** :
  * `front/src/app/features/admin/dashboard/home/home.html` (l. 95-108).
  * `front/src/app/features/admin/dashboard/home/home.ts`.
* **Spécification Technique & Préservation Produit** :
  * Remplacer ces deux cartes par des indicateurs orientés vers la valeur pédagogique :
    1. **« Participants Accueillis »** : nombre cumulé d'élèves ayant participé aux quiz de l'enseignant.
    2. **« Taux de Réussite Moyen »** : pourcentage moyen de réponses correctes de l'ensemble des sessions.
  * Conserver le style épuré et rassurant du thème QueZap.
* **Critères d'Acceptation Vérifiables** :
  1. `grep -rn 'SSE Direct' front/src/app/features/admin/dashboard/` retourne 0 occurrence.
  2. Le tableau de bord affiche des métriques d'usage réelles valorisantes pour l'enseignant.

---

#### 📋 Fiche P1-8 : Refonte Ergonomique et Spatiale du Reveal Mobile Élève
* **Axe** : UI/UX & Ergonomie Mobile.
* **Rôle Assigné** : UI/UX Designer (E2).
* **Constat & Preuves Visuelles** :
  * Fait **F9** : Sur le smartphone de l'élève (`player_07_reveal_result.png`, `player_07b_reveal_result_other.png`), le panneau « Résultats du tour » est positionné en `fixed inset-x-4 bottom-6 z-50` (`quizz-runner.html` l. 54). Il vient recouvrir maladroitement les boutons de réponse et le timer, créant une surcharge visuelle et masquant les choix sur lesquels l'élève venait d'appuyer.
* **Fichiers Cibles Vérifiés** :
  * `front/src/app/features/quizz/pages/quizz-runner/quizz-runner.html` (l. 50-95).
  * `front/src/app/features/quizz/pages/quizz-runner/quizz-runner.ts`.
* **Spécification Technique & Préservation Produit** :
  * Repenser la mise en page de la phase de révélation :
    * Masquer les boutons d'action de vote désormais inactifs.
    * Afficher un bandeau d'en-tête clair : statut **« Bien joué ! 🎉 »** (fond vert) ou **« Oups, pas tout à fait ! 💡 »** (fond ambré).
    * Présenter la carte de synthèse (points gagnés, score total, rang) au centre de l'écran avec une typographie généreuse.
    * Afficher l'explication pédagogique dans un encadré bien contrasté.
* **Critères d'Acceptation Vérifiables** :
  1. Aucun élément de texte ne se superpose lors du reveal mobile.
  2. Le résultat personnel de l'élève (gagné / perdu) est immédiatement identifiable en un coup d'œil (< 500 ms).
  3. L'explication pédagogique est lisible sans scroll horizontal.

---

### 3.3 🟢 Priorité P2 : Robustesse Technique, A11y & Architecture Hexagonale (~2 à 3 semaines)

#### 📋 Fiche P2-1 : Conformité d'Accessibilité (Sémantique Radio, Annonces `aria-live`, `prefers-reduced-motion`)
* **Axe** : QA & Accessibilité (RGAA / WCAG 2.2).
* **Rôle Assigné** : QA Specialist (E4).
* **Constat & Preuves Visuelles** :
  * Fait **F16** :
    * Dans `question-answer.html` (l. 5-10), les options de réponse utilisent `<input type="checkbox" [multiple]="!radio()">` : pour les questions à choix unique, les synthèses vocales annoncent « case à cocher » au lieu de « bouton radio », ce qui induit en erreur les utilisateurs malvoyants.
    * Aucun conteneur `aria-live` n'est configuré dans `quizz-runner` : le décompte du timer, l'envoi de la réponse et la révélation ne sont pas vocalisés.
    * Aucune directive `@media (prefers-reduced-motion: reduce)` n'existe dans le CSS pour couper les confettis et animations pulsées.
* **Fichiers Cibles Vérifiés** :
  * `front/src/app/features/quizz/components/question-view/parts/question-answer/question-answer.html` (l. 1-12).
  * `front/src/app/features/quizz/pages/quizz-runner/quizz-runner.html`.
  * `front/src/styles/index.css` & `front/src/styles/tailwind.css`.
* **Spécification Technique & Préservation Produit** :
  * Dans `question-answer.html`, utiliser sémantiquement `role="radiogroup"` sur le conteneur et `role="radio"` / `<input type="radio">` lorsque la question est à choix unique.
  * Ajouter une région `aria-live="polite" aria-atomic="true" class="sr-only"` annonçant textuellement les changements d'état majeurs (« Question 2 sur 5 », « Votre réponse a bien été transmise », « Temps écoulé »).
  * Ajouter dans `index.css` :
    ```css
    @media (prefers-reduced-motion: reduce) {
      *, ::before, ::after {
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important;
      }
    }
    ```
* **Critères d'Acceptation Vérifiables** :
  1. Un audit avec lecteur d'écran (ou inspection DOM) confirme que les choix uniques ont le rôle sémantique `radio`.
  2. En activant l'option système « Réduire les animations », tous les effets de rebond (`animate-bounce`), de pulsation (`animate-pulse`) et les confettis du podium sont désactivés.
  3. Les événements de vote sont vocalisés via `aria-live`.

---

#### 📋 Fiche P2-2 : Sanitisation Robuste des Pseudonymes et Sécurisation de l'Export CSV (Anti-Injection)
* **Axe** : Sécurité Applicative & QA.
* **Rôle Assigné** : Lead Software Engineer (E3) & QA Specialist (E4).
* **Constat & Preuves Visuelles** :
  * Fait **F7** : Dans `sim2.log` (test XSSNAME), le pseudo `<script>alert(1)</script>` a été accepté avec un statut HTTP 200 par `POST /sessions/join`. Bien qu'Angular neutralise l'exécution script à l'affichage via son sanitizer de template, la valeur brute est persistée en base.
  * Dans `SessionController.exportCsv` (l. 700-710), la méthode concatène directement `p.getName()` sans échappement des séparateurs ni protection contre l'injection de formules tableur (OWASP CSV Injection : pseudos commençant par `=`, `+`, `-`, `@`).
* **Fichiers Cibles Vérifiés** :
  * `back/modules/infrastructure/src/main/java/com/quezap/infrastructure/adapter/services/UserNameSanitizerImpl.java`.
  * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (l. 295-300, 685-715).
  * `back/quezap-api/src/test/java/com/quezap/api/v1/routes/SessionControllerTest.java`.
* **Spécification Technique & Préservation Produit** :
  * Dans `UserNameSanitizerImpl`, enrichir le nettoyage pour rejeter ou supprimer les balises HTML (`<`, `>`) et caractères de contrôle.
  * Dans `exportCsv`, appliquer la règle de neutralisation des formules tableur :
    * Si le nom commence par `=`, `+`, `-`, `@`, tab ou retour chariot, le préfixer d'une apostrophe `'`.
    * Encadrer le champ de guillemets doubles et doubler les guillemets internes (`"nom"`).
* **Critères d'Acceptation Vérifiables** :
  1. `POST /apiv1/sessions/join` avec un pseudo contenant des balises HTML retourne HTTP 400 Bad Request.
  2. L'export CSV généré pour un élève nommé `=cmd|' /C calc'!A0` produit `'=cmd|' /C calc'!A0` dans le fichier, neutralisant toute exécution dans Microsoft Excel ou LibreOffice Calc.
  3. `./back/gradlew -p back test` valide les tests de non-régression de l'export CSV.

---

#### 📋 Fiche P2-3 : Résilience SSE Avancée (Reconnexion Hôte, Rejeu `Last-Event-ID`, Extension Keep-Alive)
* **Axe** : Technique / Lead Dev.
* **Rôle Assigné** : Lead Software Engineer (E3).
* **Constat & Preuves Visuelles** :
  * Fait **F14** :
    * `SessionSseService.java` (l. 25) utilise un timeout dur de 30 minutes (`DEFAULT_TIMEOUT_MS = 30 * 60 * 1000L`). Dans le cadre d'un cours scolaire de 55 minutes, la session SSE expire au milieu du cours.
    * L'implémentation garde les émetteurs en mémoire vive sans tampon circulaire d'événements. Si une micro-coupure Wi-Fi survient, les événements manqués ne sont pas rejoués car l'en-tête standard `Last-Event-ID` n'est pas géré.
    * Côté écran hôte (`session-presenter.ts` l. 333-366), aucun gestionnaire `eventSource.onerror` n'est configuré pour orchestrer la reconnexion.
* **Fichiers Cibles Vérifiés** :
  * `back/quezap-api/src/main/java/com/quezap/api/v1/services/SessionSseService.java` (l. 20-65).
  * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java` (l. 986-1003).
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l. 333-366).
* **Spécification Technique & Préservation Produit** :
  * Porter le timeout SSE à 2 heures (`120 * 60 * 1000L`) et implémenter un battement de cœur régulier (heartbeat SSE `:keep-alive\n\n` toutes les 15 secondes) pour maintenir les proxys et pare-feux ouverts.
  * Dans `SessionSseService`, conserver un tampon en anneau des 50 derniers événements par session avec leur identifiant de séquence.
  * Sur réception du header `Last-Event-ID` dans `streamEvents`, renvoyer automatiquement les événements manqués avant les nouveaux.
  * Dans `session-presenter.ts`, ajouter un gestionnaire de reconnexion avec backoff exponentiel en cas d'erreur de socket.
* **Critères d'Acceptation Vérifiables** :
  1. Une session active reste connectée et opérationnelle au-delà de 45 minutes sans déconnexion.
  2. En coupant temporairement le réseau local pendant 5 secondes puis en le rétablissant, l'écran hôte se reconnecte sans recharger la page et rattrape les votes manqués via `Last-Event-ID`.

---

#### 📋 Fiche P2-4 : Conformité Stricte AGENTS.md Frontend (Purge des DTOs Artisans & Validation Hey-API/Zod)
* **Axe** : Technique / Lead Dev & Qualité Code.
* **Rôle Assigné** : Lead Software Engineer (E3).
* **Constat & Preuves Visuelles** :
  * Fait **F15** : Présence de DTOs codés à la main en violation explicite des règles `AGENTS.md` (§ Rôle d'Anti-Corruption Layer des services) :
    * `front/src/app/features/admin/sessions/services/session.ts` (l. 7) : `interface SessionApiResponse`.
    * `front/src/app/features/admin/sessions/services/session-http.ts` (l. 14) : `interface PageOfApiResponse<T>`.
    * `front/src/app/features/quizz/services/session-api/session-api-http.ts` (l. 27) : `interface JoinSessionApiResponse`.
    * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts` (l. 25-80) : `SseEnvelope`, `SsePayload`, `SseQuestion`, `SseAnswer`.
    * `front/src/app/features/admin/themes/services/theme-http.ts` (l. 62) : forçage de type `as unknown as { uuid?: string }`.
* **Fichiers Cibles Vérifiés** :
  * `front/src/app/features/admin/sessions/services/session.ts`.
  * `front/src/app/features/admin/sessions/services/session-http.ts`.
  * `front/src/app/features/quizz/services/session-api/session-api-http.ts`.
  * `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.ts`.
  * `front/src/app/features/admin/themes/services/theme-http.ts`.
* **Spécification Technique & Préservation Produit** :
  * Supprimer l'ensemble des interfaces artisanales susmentionnées.
  * Importer et utiliser exclusivement les types DTO générés par Hey-API dans `@quezap/core/api-client` (`SessionDetailDto`, `JoinSessionResponseDto`, `ThemeDto`, etc.).
  * Supprimer tous les forçages de type `as unknown as`.
  * Exploiter les fonctions prêtes à l'emploi du SDK Hey-API ou typer les appels directs `client.get<{ 200: MonDto }>` comme prescrit dans `AGENTS.md`.
* **Critères d'Acceptation Vérifiables** :
  1. `grep -rn 'interface SessionApiResponse' front/src/` retourne 0 occurrence.
  2. `grep -rn 'interface PageOfApiResponse' front/src/` retourne 0 occurrence.
  3. `grep -rn 'as unknown as' front/src/` retourne 0 occurrence.
  4. `cd front && bun run lint && bun run build` compile avec succès sans avertissement.

---

#### 📋 Fiche P2-5 : Découpage Hexagonal de `SessionController` & Runtime Hints GraalVM
* **Axe** : Architecture Hexagonale & Performance Native.
* **Rôle Assigné** : Lead Software Engineer (E3).
* **Constat & Preuves Visuelles** :
  * Fait **F13** : `SessionController.java` fait 1186 lignes et injecte directement des adaptateurs de persistance JPA (`SessionJpaRepository`, `QuezapJpaRepository`, `UserJpaRepository` l. 78-92). La logique métier (orchestration, calcul de progression, gestion d'état) est logée dans l'adaptateur primaire REST plutôt que dans des cas d'utilisation (`@Usecase` dans `modules:application`), ce qui contrevient au découpage hexagonal de QueZap.
  * Risque AOT GraalVM : Les nouveaux DTOs de transport d'événements SSE (`com.quezap.api.v1.dto.events.*`) doivent être dûment déclarés dans les AOT Runtime Hints (`QuezapRuntimeHints.java`).
* **Fichiers Cibles Vérifiés** :
  * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java`.
  * `back/modules/application/src/main/java/com/quezap/application/usecases/sessions/` (création des UseCases nécessaires : `SubmitAnswerHandler`, `RevealSlideHandler`, `ExportSessionReportHandler`).
  * `back/modules/infrastructure/src/main/java/com/quezap/infrastructure/aot/QuezapRuntimeHints.java` (l. 30-50).
* **Spécification Technique & Préservation Produit** :
  * Extraire la logique métier vers des handlers applicatifs purs dans `modules:application` manipulant les ports de domaine.
  * Réduire `SessionController` à son rôle strict d'adaptateur REST : validation des requêtes HTTP, délégation aux UseCases et conversion vers DTOs de réponse.
  * Enregistrer l'ensemble des DTOs SSE dans `QuezapRuntimeHints.java` via `hints.reflection().registerType(...)` avec `MemberCategory.INVOKE_DECLARED_CONSTRUCTORS` et `DECLARED_FIELDS`.
* **Critères d'Acceptation Vérifiables** :
  1. `SessionController.java` ne contient aucun import direct de `com.quezap.infrastructure.adapter.persistence.jpa.repositories.*`.
  2. Toutes les opérations métier passent par un UseCase de `com.quezap.application.usecases.*`.
  3. `./back/gradlew -p back clean test -Pnullaway=true` réussit avec 0 erreur.
  4. La compilation native GraalVM AOT s'exécute sans omission de réflexion sur les événements SSE.

---

#### 📋 Fiche P2-6 : Suite de Tests E2E Playwright Automatisée de Simulation de Classe
* **Axe** : QA & Fiabilité Temps Réel.
* **Rôle Assigné** : QA Specialist (E4) & Lead Dev (E3).
* **Constat & Preuves Visuelles** :
  * Exigence R1 & Blackboard §1.1 : La simulation multi-clients ayant permis de valider la couverture visuelle et les métriques de charge (`.audit/sim2.cjs`) a été exécutée sous forme de script Node.js autonome. Pour garantir la non-régression continue, cette simulation doit être institutionnalisée dans la suite de tests E2E du projet (`front/tests/e2e/`).
* **Fichiers Cibles Vérifiés (Existants)** :
  * `.audit/sim2.cjs` (source de référence éprouvée de simulation multi-clients et scénarios de test).
  * `front/package.json` (pour ajout de la dépendance `@playwright/test` et du script npm).
* **Fichiers à Créer / Instancier dans le cadre du chantier** :
  * `front/playwright.config.ts` (configuration globale Playwright selon les directives `.agents/skills/e2e-playwright/SKILL.md`).
  * `front/tests/e2e/classroom-simulation.spec.ts` (suite de tests de simulation de classe multi-onglets).
* **Spécification Technique & Préservation Produit** :
  * Ajouter la dépendance `@playwright/test` dans `devDependencies` et déclarer le script `"test:e2e": "playwright test"` dans `front/package.json`.
  * Instancier `front/playwright.config.ts` adapté à l'application Angular.
  * Intégrer un test Playwright complet (`front/tests/e2e/classroom-simulation.spec.ts`) simulant :
    1. Un contexte navigateur hôte (fenêtre 1920×1080) créant et projetant une session.
    2. Trois contextes navigateurs élèves mobiles (iPhone 14, 390×844) scannant le code PIN, choisissant leur pseudo et rejoignant le lobby.
    3. Trente-deux bots HTTP concurrents rejoignant la session et votant simultanément dès émission de `QUESTION_START`.
    4. Révélation de la question, vérification des scores intermédiaires, passage au podium et fin de partie.
* **Critères d'Acceptation Vérifiables** :
  1. En validation immédiate, l'exécution du script de référence autonome `node .audit/sim2.cjs` (ou `bun .audit/sim2.cjs`) s'exécute avec succès.
  2. À l'issue du chantier et de la configuration de `package.json`, la commande pérenne `cd front && bun run test:e2e` exécute la simulation de classe complète sans échec ni timeout en mode headless.
  3. Le test valide la réception des événements SSE pour l'ensemble des 35 participants en moins de 2 secondes.

---

#### 📋 Fiche P2-7 : Arbitrage Produit & Verrouillage de la Politique d'Accès Public aux Quezaps
* **Axe** : Commercial / CPO & Sécurité Applicative.
* **Rôle Assigné** : CPO (E1) & Lead Dev (E3).
* **Constat & Preuves Visuelles** :
  * Fait **F20** : Dans `SecurityConfig.java` (l. 60), la route `GET /apiv1/quezaps/**` est en accès libre `permitAll()`, alors que `GET /apiv1/sessions` est verrouillée par `authenticated()`.
  * Tout visiteur anonyme peut donc lister et lire l'ensemble des quiz stockés en base, y compris les questions et réponses correctes.
* **Fichiers Cibles Vérifiés (Existants)** :
  * `back/quezap-api/src/main/java/com/quezap/api/v1/security/SecurityConfig.java` (l. 60-65).
  * `back/quezap-api/src/main/java/com/quezap/api/v1/routes/quezaps/QuezapController.java` (l. 165-175).
* **Fichiers de Test à Créer dans le cadre du chantier** :
  * `back/quezap-api/src/test/java/com/quezap/api/v1/security/SecurityConfigTest.java` (classe de test MockMvc à instancier pour valider les règles d'autorisation sur `/apiv1/quezaps/**`).
* **Spécification Technique & Préservation Produit** :
  * Arbitrage CPO :
    * Si l'intention est de proposer une bibliothèque communautaire de quiz publics (modèle partage d'enseignant à enseignant) : conserver `permitAll()` sur `GET /apiv1/quezaps` mais filtrer strictement sur `isPublic == true`, et interdire la lecture des quiz privés d'un autre enseignant sans authentification.
    * Si tous les quiz doivent être strictement protégés : modifier `SecurityConfig` pour exiger une authentification JWT sur `/apiv1/quezaps/**`.
  * Créer la classe de test `SecurityConfigTest.java` sous `back/quezap-api/src/test/java/com/quezap/api/v1/security/` et adapter la suite de tests MockMvc.
* **Critères d'Acceptation Vérifiables** :
  1. Vérification HTTP immédiate (lorsque l'API backend est active) : la commande `curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/apiv1/quezaps` retourne le code HTTP attendu selon l'arbitrage retenu (code `200` avec liste filtrée des quiz publics uniquement si `permitAll()` restreint aux données publiques est conservé, ou code `401` si l'authentification obligatoire est actée).
  2. Création et exécution de la classe de test dédiée : `./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.security.SecurityConfigTest"` confirme la règle retenue sans régression.

---

#### 📋 Fiche P2-8 : Harmonisation Transverse des Tokens de Design (Tailwind @theme & PrimeNG Aura)
* **Axe** : UI/UX & Design System.
* **Rôle Assigné** : UI/UX Designer (E2) & Lead Dev (E3).
* **Constat & Preuves Visuelles** :
  * Débat E2/E3 (Blackboard §3) : Deux sous-systèmes de tokens de couleurs coexistent :
    1. Les tokens Tailwind CSS v4 dans `front/src/styles/quezap-theme/colors.css` (`--color-brand-green-default`, `--color-brand-yellow-default`, `--color-ui-primary-default-base`).
    2. Le thème personnalisé PrimeNG Aura dans `front/src/themes/Quezap.ts`.
  * Bien qu'il n'y ait pas de conflit bloquant à l'affichage, une légère dérive chromatique existe entre les composants natifs Tailwind et les composants PrimeNG.
* **Fichiers Cibles Vérifiés** :
  * `front/src/styles/quezap-theme/colors.css` (l. 1-48).
  * `front/src/themes/Quezap.ts` (l. 4-60).
  * `front/src/styles/index.css`.
* **Spécification Technique & Préservation Produit** :
  * Mapper les variables de palette de PrimeNG (`primary.500`, `primary.600`) directement sur les variables CSS exportées par Tailwind `@theme` (`var(--color-brand-green-default)`).
  * Garantir une parité parfaite des couleurs de succès, d'avertissement et de danger sur l'ensemble de l'application sans duplication de codes hexadécimaux codés en dur.
* **Critères d'Acceptation Vérifiables** :
  1. Les composants PrimeNG (boutons, inputs, toasts) utilisent exactement les mêmes valeurs chromatiques que les composants Tailwind sans surcharge CSS ad-hoc.
  2. `cd front && bun run build` compile sans aucune régression visuelle.

---

## 4. Matrice de Validation du Consensus des 4 Experts

### 4.1 Approbation Formelle des Experts

Chacun des 4 experts mandatés a examiné l'ensemble du plan d'action directeur et a formulé sa validation explicite, assortie de ses réserves méthodologiques :

| Expert Mandaté | Rôle & Focus | Statut | Réserves & Conditions d'Exécution |
| :--- | :--- | :---: | :--- |
| **E1 — CPO** | Stratégie Commerciale, Rétention & Onboarding 30s | **VALIDÉ ✅** | **Priorité absolue sur P0-1, P0-2, P0-8 et P1-1** : L'enseignant doit pouvoir lancer un quiz sans friction et l'élève doit repartir avec son score et son rang. Condition impérative : **Ne jamais toucher à la gratuité, à l'absence de compte pour les élèves, ni au thème jardin.** |
| **E2 — UI/UX** | Lisibilité 10m, Ergonomie Tactile & Design System | **VALIDÉ ✅** | **Priorité sur P0-1 (icônes) et P1-3 (formes daltoniennes)** : La discordance des formes pénalise l'accessibilité en classe. Réserve notée : P2-8 (harmonisation poussée des tokens) doit rester **optionnel** afin de concentrer les efforts sur l'ergonomie mobile et le reveal (P1-8). |
| **E3 — Lead Dev** | Architecture Hexagonale, GraalVM & Fiabilité SSE | **VALIDÉ ✅** | **Priorité sur P0-3 (triche par réponse tardive) et P0-5 (delta de score)** : Ces bugs faussent l'équité du jeu. Condition impérative : **Le refactoring hexagonal de `SessionController` (P2-5) ne doit être entamé qu'après couverture complète par tests unitaires** pour garantir l'absence de régression. |
| **E4 — QA / A11y** | RGAA / WCAG 2.2, Intégrité Données & Edge Cases | **VALIDÉ ✅** | **Priorité sur P0-6 (validation pseudo) et P2-1 (sémantique radio/a11y)** : Réserve expresse : Les cas extrêmes de coupure réseau prolongée en mode avion et la validation sur lecteur d'écran physique (NVDA / VoiceOver) devront faire l'objet de tests sur banc réel lors du jalon P2-6. |

---

## 5. Protocole Qualité & Commandes de Vérification

Toute implémentation future issue de ce plan d'action devra être validée par l'exécution stricte de la chaîne de vérification outillée suivante, conformément aux exigences de `AGENTS.md`.

### 5.1 Vérification Backend (Java 25 / Spring Boot / Hexagone)
```bash
# 1. Formatage strict du code selon Google Java Format
./back/gradlew -p back spotlessApply

# 2. Vérification des règles Checkstyle
./back/gradlew -p back checkstyleMain checkstyleTest

# 3. Compilation stricte avec NullAway actif et exécution de tous les tests unitaires / use cases
./back/gradlew -p back clean test -Pnullaway=true

# 4. Régénération du contrat d'interface OpenAPI après toute modification de DTO / contrôleur
./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.GenerateOpenApiJsonTest"
```

### 5.2 Vérification Frontend (Angular 21 / TypeScript / Tailwind)
```bash
# 1. Synchronisation automatique du client Hey-API et des validateurs Zod
cd front && bun run codegen

# 2. Linting TypeScript et CSS strict
cd front && bun run lint:ts:fix && bun run lint:css:fix

# 3. Exécution de la suite de tests unitaires Jest
cd front && bun run test

# 4. Compilation Ahead-Of-Time (AOT) obligatoire (détection des erreurs de template et types stricts)
cd front && bun run build
```

### 5.3 Vérification E2E & Intégrité Globale
```bash
# 1. Simulation de classe multi-onglets immédiate (1 hôte + 35 élèves via le script autonome de référence)
node .audit/sim2.cjs
# (ou alternativement via Bun : bun .audit/sim2.cjs)

# 2. Suite de tests E2E Playwright pérenne (script cible configuré dans front/package.json à l'issue du chantier P2-6)
cd front && bun run test:e2e

# 3. Contrôle strict d'intégrité git (aucune modification intempestive en dehors du périmètre)
git status --short
```

---

*Fin du document de référence — Document approuvé à l'unanimité par le Conseil d'Audit QueZap.*
