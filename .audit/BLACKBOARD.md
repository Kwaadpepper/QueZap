# QueZap — BLACKBOARD d'audit (lecture seule)

> Méthode : 4 experts (E1 CPO/commercial, E2 UI/UX, E3 Lead dev, E4 QA/a11y) délibèrent en 3 tours (diagnostic → confrontation → synthèse).
> Honnêteté méthodologique : les 4 rôles ont été joués par **un seul agent** (pas de sous-agents), sur la base de preuves réellement collectées (captures, appels API, lecture de code). Le modèle utilisé n'est pas sélectionnable depuis cet agent (la demande « Claude Sonnet » n'a pu être ni honorée ni vérifiée).
> Simulation live : `.audit/sim2.cjs` (log brut : `.audit/sim2.log`). Session de test : PIN 62702 (1 hôte 1920x1080, 3 mobiles via UI 390x844, 32 élèves via API = 35 participants + 1 pseudo `<script>` posté après capture).

---

## 1. Facts & Visual Evidence

### 1.1 Couverture visuelle R1 (captures dans `.audit/screenshots/`)

| Écran demandé | Fichier | Statut |
|---|---|---|
| Lobby hôte (PIN/QR/joueurs) | `host_20_lobby_empty.png`, `host_21_lobby_35_players.png` (fullPage), `host_21b_lobby_35_viewport.png` | ✅ |
| Question en cours (hôte) | `host_22_question_running.png`, `host_23_question_votes_counting.png`, `host_24_question_after_host_reload.png`, `host_26_question_2.png`, `host_27_question_3.png` | ✅ |
| Reveal (hôte) | `host_25_reveal.png`, `host_28_reveal_2.png`, `host_29_reveal_3.png` | ✅ |
| Classement intermédiaire (hôte) | **Absent par conception** : `INTERMEDIATE_LEADERBOARD` n'est pas dans le `switch` de `handleEvent` (`session-presenter.ts` l. ~385-450) — il est seulement dans la liste d'écoute (l. ~358). Aucun écran n'existe à capturer. | ⚠️ justifié |
| Podium (hôte) | `host_30_podium.png` | ✅ |
| Rapport de session | `host_31_session_report.png` (fullPage) | ✅ |
| Élève : join prérempli / pseudo | `player_01_join_prefilled.png`, `player_02_nickname_filled.png` (⚠️ pseudo non rempli à cause d'un sélecteur erroné de ma première version du script ; voir `player_03b_lobby_long_name.png` pour le cas pseudo long) | ✅ partiel |
| Élève : lobby | `player_03_lobby.png`, `player_03b_lobby_long_name.png`, `player_03c_lobby_35.png` | ✅ |
| Élève : vote actif | `player_04_active_vote_boolean.png`, `player_08_question_2.png`, `player_08_question_3.png` (QCM 4 choix) | ✅ |
| Élève : feedback | `player_05_feedback_after_vote.png`, `player_07_reveal_result.png`, `player_07b_reveal_result_other.png`, `player_09_reveal_2/3.png` | ✅ |
| Élève : reload mid-question | `player_06_after_reload_mid_question.png` | ✅ |
| Élève : fin de partie | `player_10_end_of_game.png`, `player_10b_end_of_game_p3.png` (URL `/quizz/ended`) | ✅ |
| Existants (étape 1) | 15 captures `host_0x/1x`, `player_join*`, `player_expired/ended` (`INDEX.txt`) ; `player_lobby.png` = redirection `/quizz/expired` (garde `hasActiveSessionGuard`) | ✅ |

Note : sur `host_26/27`, `player_08_question_2` : le vote `vote2` a échoué (timeout locator, ma simulation) — la question 2 a été simplement non répondue ; sans impact sur les constats.

### 1.2 Faits mesurés (reproductibles)

| # | Fait | Preuve |
|---|---|---|
| F1 | Les icônes PrimeIcons (`pi pi-*`) **ne s'affichent nulle part** : `primeicons` n'est ni dans `front/package.json` (`grep -c primeicons` = 0) ni importé (`grep -rn primeicons front/src front/angular.json` = vide), alors que 10 templates utilisent `pi pi-…` (arrow-right ×5, check ×3, plus ×2…). Conséquences visibles : carte « Créer un Quiz » sans icône (`host_10_admin_dashboard.png`, `home.html` l.34), bouton « Démarrer le Quizz → » et « Question suivante → » sans flèche, **pastille ✓ de bonne réponse vide** (`host_25_reveal.png` : rond vert sans coche), bouton « Plein écran » sans icône. | grep + captures |
| F2 | L'hôte affiche l'URL **« quezap.fr »** en dur (`session-presenter.html` l.~36) alors que le lien réel est `window.location.origin + /quizz/join/{code}` (`session-presenter.ts` `joinUrl`). Saisir quezap.fr + PIN ne fonctionne pas : le PIN se saisit sur `/quizz/join`. | `host_20_lobby_empty.png`, code |
| F3 | Question BOOLEAN : l'API ne stocke **qu'une seule réponse** `"True"` (`GET /apiv1/quezaps/{id}` → `answers:[{label:"True",isCorrect:true}]`). L'élève voit « Vrai / Faux », **l'hôte voit une seule carte « True »** (anglais) — pas de « Faux ». | `host_22_question_running.png`, `player_04_active_vote_boolean.png`, sortie API dans `sim2.log`/réponse quezap |
| F4 | Les symboles de réponse **diffèrent entre hôte et élève** : hôte ▲◆●■ en rouge/bleu/jaune/vert (`getSymbol`, `getChoiceColorClass`), mobile étoile/triangle (Vrai/Faux) puis cercle/croix/losange/hexagone (`player_08_question_3.png`). La classe ne peut pas dire « je clique sur la forme que je vois au tableau ». | captures hôte vs mobile |
| F5 | **Réponse acceptée après le reveal** : `POST /apiv1/sessions/{id}/answers` après `QUESTION_REVEAL` → `201 {"accepted":true}` (log `LATE ANSWER AFTER REVEAL 201`). `submitAnswer` (`SessionController.java` l.515-650) ne contrôle ni l'échéance du timer ni l'état « révélé ». | `sim2.log` |
| F6 | 30 réponses concurrentes : 30×201 en 166 ms, aucun conflit (`CONCURRENT 30 ANSWERS {"201":30}`) — robuste à cette charge. Mais chaque réponse charge le graphe complet `sessionJpa.getAnswers()/getParticipants()` (O(n) par requête, l.540-640). | `sim2.log`, code |
| F7 | Pseudo `<script>alert(1)</script>` **accepté** (200) par `POST /sessions/join` ; pseudo 300 car. → 400 « between 2 and 50 characters » ; doublon `léa`/`Léa` → 409. Angular échappe à l'affichage, mais le CSV (`export/csv`, l.666) et tout autre consommateur reçoivent la valeur brute (injection de formules `=`/`+` non testée, à vérifier). | `sim2.log` (XSSNAME, HUGENAME, DUPNAME) |
| F8 | Pseudo de 52 caractères côté UI : le bouton « Rejoindre le salon » reste grisé/vert pâle, **aucun message d'erreur visible** ; la requête 400 part quand même (console `P1ERR … 400`). L'élève reste bloqué sans explication. | `player_03b_lobby_long_name.png`, `sim2.log` |
| F9 | Écran de reveal mobile : bandeau **« Dépêchez-vous ! Il ne reste que 4s »** encore affiché après la révélation ; carte « Résultats du tour » superposée aux réponses ; **« Points gagnés : 0 pts » alors que le score total passe à 906** (Léa a bien répondu juste). Cause probable : le backend envoie `delta = score` et `streak = 1` en dur (`SessionController.java` `broadcastRevealAndLeaderboard` : `new LeaderboardEntryDto(rank++, name, score, p.getScore(), 1)`), le front calcule `score - previousScore` (`quizz-runner.ts` l.60-64). | `player_07_reveal_result.png`, code |
| F10 | Fin de partie élève : `/quizz/ended` affiche « Bravo, Léa ! » + formulaire PIN **pré-rempli « 20082 » en rouge** ; **ni rang final ni score** (alors que `SESSION_ENDED` contient `finalLeaderboard`+`podium`). | `player_10_end_of_game.png`, `SessionEndedPayload.java` |
| F11 | Reload hôte en pleine question : le compteur de réponses repasse à **« 0 / 35 »** (31 réponses déjà reçues) et le timer **repart de la durée serveur** (18 s) ; l'hôte n'a pas de `Last-Event-ID` ni d'état serveur du temps restant (`session-presenter.ts` `startCountdown` local `setInterval`, `connectSse` sans `onerror`). Côté élève la reprise d'état existe (garde + `getInitialStreamData` renvoie `LOBBY_STATE`+`QUESTION_START`). | `host_24_…png`, `sim2.log` (HOST RELOAD), code |
| F12 | Aucun **auto-reveal à 0 s** côté hôte (le timer s'arrête, il faut cliquer « Révéler »). Le serveur auto-révèle seulement quand tout le monde a répondu (`totalAnswered >= totalActive`). | `session-presenter.ts` l.~290-310, `SessionController.java` l.~625 |
| F13 | `SessionController.java` fait **1185 lignes** et injecte directement `SessionJpaRepository`, `QuezapJpaRepository`, `UserJpaRepository` (l.78-80, 90-92) : la logique métier (scoring, règles de session) vit dans l'adaptateur REST et dépend d'`infrastructure`, contrairement à AGENTS.md (§ découpage hexagonal : `quezap-api` = adaptateur primaire ; handlers `@Usecase` dans `application`). `Session.calculateScore` existe dans le domaine, mais l'orchestration non. | code |
| F14 | SSE : `SessionSseService` garde émetteurs et séquences **en mémoire** (`ConcurrentHashMap`, l.25-30), pas de rejeu sur `Last-Event-ID`, séquence réinitialisée au redémarrage ; `/events` n'est pas authentifié (EventSource ne peut pas envoyer de Bearer). Timeout 30 min (`DEFAULT_TIMEOUT_MS`) : une session > 30 min coupe l'hôte et les élèves ; la reconnexion automatique du navigateur existe (`session-observer-sse.ts` l.162 `onerror`), mais côté hôte il n'y a aucun `onerror`. | code |
| F15 | Violations AGENTS.md frontend : DTOs artisanaux (`SessionApiResponse` dans `admin/sessions/services/session.ts` l.7 ; `PageOfApiResponse` dans `session-http.ts` l.14 ; `JoinSessionApiResponse` dans `quizz/services/session-api/session-api-http.ts` l.27 ; `SseEnvelope/SsePayload/SseQuestion` dans `session-presenter.ts` l.~25-80) ; `HttpClient` direct dans 4 fichiers au lieu du SDK Hey-API (`session-http.ts`, `session-api-http.ts`, `session-report.ts`, `session-presenter.ts`) ; `as unknown as` dans `theme-http.ts` l.62 ; casts `as { … }` sur payload SSE dans `session-presenter.ts` (QUESTION_REVEAL / ANSWER_RECEIVED). Pas de `any` hors code généré. | grep |
| F16 | Accessibilité : `<html lang="en">` (`front/src/index.html` l.2) pour une app française ; **aucune** `prefers-reduced-motion` dans `front/src` (confettis, `animate-pulse`, `animate-ping`) ; **aucun** `aria-live`/`role=status|alert` dans `front/src/app` (le « Réponse transmise », le timer, les arrivées de joueurs ne sont pas annoncés) ; les réponses sont des `<input type="checkbox" [multiple]>` cachés dans un `<label>`, avec `(click)` posé sur le composant (`question-answer.html`, `quizz-question.html` l.~45) — une question à choix unique est annoncée « case à cocher » ; texte de repli `pas de reponse` en dur (`quizz-question.html`, `boolean-question.html`). | grep + code |
| F17 | Bouton « Exclure ce joueur » (kick) : `opacity-0 group-hover:opacity-100` (`session-presenter.html` l.~112) — **invisible sur écran tactile / au clavier** (pas de `group-focus-within`). Le bouton « Démarrer » est en `animate-pulse` permanent et son libellé apparaît atténué (`host_21_lobby_35_players.png`). | code + capture |
| F18 | Tableau de bord admin : les cartes de stats affichent « MODE PROJECTION 16:9 HD » et « PROTOCOLE TEMPS RÉEL SSE Direct » (jargon technique sans valeur pour un enseignant) ; `Total quiz 14`, `Sessions créées 32`. La liste « Mes Quezaps » est polluée de doublons de seed « Grand Quiz Découverte NNNN ». | `host_10_admin_dashboard.png`, `host_13_admin_quezaps.png` |
| F19 | Landing : placeholder PIN `20082` rendu **en rouge** comme une valeur/erreur (`host_00_landing.png`, `join-form.html` classe `text-…` du champ ; idem `nickname-value` placeholder rouge dans `player_02_nickname_filled.png`) ; propositions de valeur sobres, pas de démo/visuel produit ; promesse « gratuit, sans compte pour les participants » correcte et bonne. | captures |
| F20 | `GET /apiv1/quezaps` répond **200 sans authentification** (liste des quezaps publics — `isPublic:true`) ; `/apiv1/sessions` → 401. À confirmer côté intention produit (catalogue public ?). | `curl` |
| F21 | **État du dépôt avant mon intervention** : `git status` montrait déjà 9 fichiers suivis modifiés *hors `.audit/`* (SessionController.java, SessionControllerTest.java, join-form.html/.ts, binary/boolean/quizz-question.html, question-icon.css, questions.css) — travail en cours d'un autre intervenant, non lié à cet audit. | `git status --short` exécuté avant toute écriture |

---

## 2. Specialist Contributions (Tour 1 — Diagnostic)

### E1 — CPO & stratégie commerciale
- **Atouts réels à protéger** : élève sans compte (F19 : « sans création de compte pour le public »), PIN + QR en 2 s, projection plein écran 16:9, podium avec confettis (`host_30_podium.png`), export CSV (`host_31_session_report.png`). Face à Kahoot/Quizizz/Wooclap : gratuit + francophone + RGPD-simple = positionnement « l'outil de l'enseignant français ».
- **Frictions onboarding 30 s** : (a) la première chose vue par l'hôte au lobby est « quezap.fr » (F2) — risque de crédibilité immédiat ; (b) icônes invisibles (F1) → impression de produit cassé dès le dashboard ; (c) le bouton « Lancer » depuis Mes Quezaps ouvre-t-il directement la session ? Il faut aujourd'hui créer Session > choisir Quezap (`session-create.html`) : 3 écrans pour jouer ; (d) dashboard avec jargon (F18).
- **Moments de rétention manqués** : l'élève ne voit **ni son rang ni son score final** (F10), pas de « Rejouer / partager », pas de récap par question pour l'enseignant.
- Proposition : « Lancer » en 1 clic depuis la carte quezap, rang final élève, reco-lien correct à l'écran, nettoyage du dashboard. Interdit de toucher à : gratuité, anonymat, thème « jardin » (identité).

### E2 — UI/UX & design system
- **Lisibilité à 10 m** : PIN géant et contrasté (excellent, `host_20_*`), question en 60-72 px OK ; **mais** libellés de réponses hôte `text-2xl/3xl` tronqués (`truncate`, `session-presenter.html` carte reveal) et compteur « 0 / 35 réponses » `text-xl` trop petit à 10 m ; badge pseudo `text-xl` dans le lobby à 35 joueurs OK mais le bouton Démarrer sort sous le pli quand >~30 joueurs si la zone joueurs grandit (`host_21_lobby_35_players.png` fullPage vs viewport).
- **Thumb zone** : les réponses mobiles occupent bien l'écran et sont grandes (`player_08_question_3.png`) ✅ ; le bouton « X » de sortie est en haut à droite (zone difficile + risque de sortie accidentelle sans confirmation visible sur les captures) ; le timer circulaire bas-droit **recouvre** le bandeau « Réponse transmise » (`player_05_feedback_after_vote.png`).
- **Cohérence** : deux systèmes visuels (hôte sombre/indigo en Tailwind pur vs mobile « jardin » + classes custom `questions.css` vs admin clair PrimeNG) ; symboles hôte ≠ élève (F4) ; libellé « True » vs « Vrai » (F3) ; titres de question mobile en MAJUSCULES (lisibilité moindre pour longues phrases).
- **États** : reveal mobile surchargé (F9) ; `/quizz/ended` ré-affiche un formulaire PIN pré-rempli (F10) ; placeholders rouges (F19).

### E3 — Lead engineer (AGENTS.md, SSE, faisabilité)
- **Conformité** : F13 (contrôleur de 1185 lignes sur JPA direct), F15 (DTOs artisanaux, HttpClient direct, casts), manque probable de **runtime hints AOT** pour les nouveaux payloads SSE (`com.quezap.api.v1.dto.events.*`) — à vérifier dans `QuezapRuntimeHints` avant tout build natif.
- **Robustesse** : F5 (réponses tardives acceptées → triche + scores faussés), F9 (delta/streak faux), F11/F12 (état hôte non restauré, pas d'auto-reveal), F14 (SSE mémoire/non-rejouable/30 min), `startQuiz()` passe l'UI en `QUESTION` **avant** la réponse serveur (`session-presenter.ts` `startQuiz`) — en cas d'erreur 4xx/5xx l'écran est faux. `ngOnInit` retombe sur un UUID codé en dur `018e3a2b-8888-7def-8123-abcdef888888` si pas de paramètre (l.~129) — reliquat de mock.
- **Performance** : F6 (O(n) par réponse) acceptable à 35, à surveiller à 200+ ; `@Transactional` + `sessionJpa.getAnswers()` pagine tout l'historique de la session à chaque réponse.
- **Faisabilité** : F1 = 1 dépendance ou 10 icônes remappées vers `quizz-icon` ; F2 = 1 ligne ; F9 = backend (calcul du delta) + test ; F5 = 3 garde-fous dans `submitAnswer` ; refactor F13 = gros chantier → à découper (P2) sans casser l'API (OpenAPI figé).

### E4 — QA & accessibilité
- **Daltonisme** : forme + couleur présentes côté élève ✅ (cercle/croix/losange/hexagone, `player_08_question_3.png`) mais **la forme n'est pas la même que sur l'écran hôte** (F4) → l'aide aux daltoniens est annulée. Reveal : seule la bordure verte + pastille (sans coche à cause de F1) indiquent la bonne réponse sur l'écran hôte, les autres cartes sont atténuées `opacity-40` : à contraste faible c'est insuffisant → ajouter un libellé « ✔ Bonne réponse » textuel.
- **Cas limites testés** : pseudo 52 car. (F8), 300 car. (400 OK), doublon (409 OK), `<script>` (F7), 35 élèves (OK : `host_21_*`, 30 réponses/166 ms), reload élève (OK : `player_06_*`), reload hôte (F11), réponse tardive (F5).
- **Cas non testés (limites de ma simulation)** : coupure réseau réelle/mode avion, plusieurs onglets avec le même pseudo, session > 30 min, 100+ élèves, lecteur d'écran réel (NVDA/VoiceOver), navigation clavier hôte.
- **A11y** : F16, F17, contraste du texte « Démarrer le Quizz » atténué par `animate-pulse`, boutons icône sans nom accessible (`buttons: ["",""]` relevé sur le mobile en jeu : le bouton « X » et un second bouton sans libellé ; `sim2.log` ligne `P0 buttons`).

---

## 3. Cross-Examination & Debates (Tour 2 — Confrontation)

**E1 → E3** : « Un refactor hexagonal de `SessionController` est-il vendable ? » — **E3** : non en P0/P1 ; invisible du client. Mais **F5** (triche par réponse tardive) est un risque produit réel en classe → P0 serveur, petit patch dans le contrôleur actuel. Le refactor reste P2 avec filet de non-régression (`SessionControllerTest`). **E1 accepte**, à condition que F5/F9 sortent avant.

**E1 → E2** : « Refaire l'écran hôte pour la vente ? » — **E2** : non, il est bon (PIN, podium). Seules corrections ciblées : URL (F2), icônes (F1), libellés. **E1 accepte** : ne pas « dénaturer ».

**E2 → E4** : « Unifier les symboles : lesquels garder ? » — **E4** : garder le jeu **mobile** (cercle/croix/losange/hexagone, 4 couleurs, plus distincts en silhouette que ▲◆●■ en texte), et l'importer côté hôte via le même composant `quizz-question-icon` (partage de SVG), avec numéro d'ordre. **E2 d'accord**, **E3** : faisable (le composant existe, `question-icon.ts`), mais il utilise `HttpClient` pour charger des SVG (F15) → à vérifier.

**E3 → E2** : « Auto-reveal à 0 s : surprise pour l'enseignant ? » — **E2** : proposer un réglage « Révélation automatique » (activé par défaut, désactivable) + compte à rebours visible ; **E1** : c'est un argument face à Kahoot (rythme). **E3** : le serveur doit être source de vérité du temps (`currentSlideStartedAt` existe déjà, `SessionController` l.~575) — émettre `deadline` ISO dans `QUESTION_START`. **Consensus** (P1).

**E4 → E3** : « XSS pseudo : bloquer côté serveur ou seulement échapper ? » — **E3** : Angular échappe, le vrai risque est le CSV/PDF/rapport ; faire **les deux** : sanitizer serveur (rejette `<`, `>`, préfixe `=+-@` dans le CSV) + test. **E4** vérifiera par commande.

**E4 → E1** : « Ajouter un rang final élève est-il hors-périmètre ? » — **E1** : non, c'est la rétention (partage, « je veux rejouer »). Les données existent déjà dans `SESSION_ENDED` (F10). **Consensus** P0/P1 selon effort : P0 = afficher rang + score.

**E1 → E4** : « 35 élèves : problème côté hôte ? » — **E4** : liste OK mais Start sous le pli sans scroll ; recommander sticky footer de lobby. **E2** : oui, bouton Démarrer sticky + compteur.

**E3 → E1** : « Rendre `GET /quezaps` public est-il voulu ? » — **E1** : catalogue public = atout futur (templates communautaires) mais à décider ; ne rien changer sans décision produit → P2 « clarifier », aucune modification.

**Désaccord résiduel** (non tranché) : E2 voudrait un thème unique (tailwind tokens) pour hôte/mobile/admin ; E3 estime le coût disproportionné (P2 seulement, via tokens CSS déjà dans `front/src/styles/quezap-theme/colors.css`). Conservé en P2 « optionnel ».

---

## 4. Consensus & Master Action Plan (Tour 3 — Synthèse)

Priorisation retenue (détails, fichiers, critères : `.audit/ACTION_PLAN.md`) :

**P0 (bloquants / quick wins, ~1-2 j)** : P0-1 icônes (F1) · P0-2 URL de join réelle (F2) · P0-3 réponses tardives (F5) · P0-4 libellés Vrai/Faux sur l'hôte (F3) · P0-5 delta/streak/“Points gagnés” (F9) · P0-6 message d'erreur pseudo invalide + validation alignée 2-50 (F8) · P0-7 placeholders non rouges + `lang="fr"` (F16, F19) · P0-8 rang/score final élève (F10).

**P1 (évolutions produit, ~1-2 sem.)** : P1-1 lancement en 1 clic depuis un Quezap · P1-2 timer serveur (`deadline`) + auto-reveal · P1-3 symboles/couleurs unifiés hôte/mobile · P1-4 restauration d'état hôte (compteur, classement) · P1-5 écran de classement intermédiaire hôte · P1-6 lobby : bouton Démarrer sticky + kick tactile · P1-7 dashboard : retirer le jargon, vraies métriques · P1-8 reveal mobile simplifié.

**P2 (robustesse technique / a11y / edge cases)** : P2-1 a11y (radio/aria-live/reduced-motion) · P2-2 sanitisation pseudo + CSV · P2-3 SSE robuste (replay `Last-Event-ID`, onerror hôte, keep-alive > 30 min) · P2-4 conformité Hey-API/Zod (supprimer DTOs artisanaux) · P2-5 extraction use cases hexagonaux de `SessionController` + AOT hints · P2-6 tests E2E Playwright automatisés de la simulation + test de charge · P2-7 clarifier `GET /quezaps` public · P2-8 tokens de design unifiés (optionnel).

### Validation du consensus (chaque expert)
- **E1 (CPO)** : ✅ validé — P0-1/2/8 et P1-1 couvrent l'onboarding ; rien ne touche gratuité/anonymat/identité « jardin ».
- **E2 (UI/UX)** : ✅ validé — réserve notée : P2-8 (tokens unifiés) reste optionnel.
- **E3 (Lead dev)** : ✅ validé — à condition que P2-5 soit découpé et protégé par tests avant refactor ; P0-3/P0-5 d'abord (risque métier).
- **E4 (QA/a11y)** : ✅ validé — ajoute la réserve que les cas « réseau coupé réel » et « lecteur d'écran réel » n'ont pas été testés (voir ACTION_PLAN P2-1/P2-6 pour les couvrir) ; le consensus est donc valide sur ce qui a été mesuré, pas sur ces deux zones.

## Annexe — résidus de test
La simulation a créé en base ~6 sessions « Audit Live NNN » (dont 3 avec des participants `Eleve1..32`, `Léa`, `Zoé 🦊`, `<script>alert(1)</script>`) via l'API. Aucune modification de code source. Scripts : `.audit/prep.cjs`, `.audit/sim1.cjs`, `.audit/sim2.cjs`, `.audit/dbg.cjs`.
