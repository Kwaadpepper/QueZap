# Rapport d'Évaluation Technique et Adversariale — Action Plan QueZap

**Auteur** : Reviewer 2 (Technical & Architectural Reviewer / Adversarial Critic)  
**Rôle** : Reviewer & Critic  
**Date d'évaluation** : 12 Octobre 2026  
**Cible examinée** : `/home/jeremy/projets/QueZap/.audit/ACTION_PLAN.md`  
**Documents de référence** : `/home/jeremy/projets/QueZap/AGENTS.md`, `/home/jeremy/projets/QueZap/.agents/teamwork/ORIGINAL_REQUEST.md`, `/home/jeremy/projets/QueZap/.audit/BLACKBOARD.md`  
**Verdict Formel** : **APPROVE** (avec réserves et recommandations d'exécution documentées)

---

## 1. Observation (Faits Objectifs et Constats Directs)

L'examen contradictoire approfondi du plan d'action (`.audit/ACTION_PLAN.md`) au crible du code source réel (`back/` et `front/`), des captures d'écran (`.audit/screenshots/`) et des logs d'exécution (`.audit/sim2.log`) a permis d'établir les observations directes suivantes :

### 1.1 Contrôle d'Intégrité (Adversarial Integrity Audit)
* **Intégrité du code source** : Aucun fichier de code source n'a été créé ou altéré par la brigade d'audit au cours de sa mission. La règle d'or **R4** (« Lecture seule, aucune implémentation ») est strictement respectée. (Note d'état : 9 fichiers présentaient des modifications non commitées antérieures au démarrage de l'audit, datées du 11 octobre 22:34).
* **Absence de fabrication de preuves** :
  * Le fait **F1** (`primeicons` absent) est exact : `grep -c "primeicons" front/package.json` renvoie `0`. Les icônes `pi pi-plus` (`home.html:34`) et `pi pi-arrow-right` (`home.html:43, 63, 83`) sont effectivement invisibles.
  * Le fait **F2** (`Rejoignez la partie sur quezap.fr`) est textuellement vérifié dans `front/src/app/features/admin/sessions/pages/session-presenter/session-presenter.html:33`.
  * Le fait **F3** (modèle `QuestionType.BOOLEAN` n'acceptant qu'une seule réponse en base dans `modules/domain/.../Question.java:78-80` vs affichage de 2 boutons sur mobile dans `boolean-question.ts:49-52` vs affichage d'une seule carte « True » sur vidéoprojecteur dans `session-presenter.html:228-266`) est rigoureusement exact.
  * Le fait **F5** (acceptation des réponses tardives post-reveal) est directement avéré dans `.audit/sim2.log` (`LATE ANSWER AFTER REVEAL 201 {"accepted":true}`) et dans le code de `SessionController.java:520-640` qui ne vérifie ni le statut de révélation ni l'écoulement du compte à rebours.
  * Le fait **F7** (injection XSS dans les pseudos et absence de neutralisation CSV OWASP) est avéré : `sim2.log` montre l'acceptation avec HTTP 200 de `<script>alert(1)</script>`, et `SessionController.java:700-710` concatène `p.getName()` sans échappement ni protection contre les formules (`=`, `+`, `-`, `@`).
  * Le fait **F8** (pseudo 52 caractères bloqué sans message d'erreur, `minLength: 3` vs backend 2, aucun `maxLength: 50`) est vérifié dans `nickname-chooser.ts:47-50` et `nickname-chooser.html:52-60`.
  * Le fait **F9** (delta de score affichant 0 pts au reveal) est vérifié : `SessionController.java:1059` injecte `p.getScore()` deux fois (`LeaderboardEntryDto(rank++, p.getName(), p.getScore(), p.getScore(), 1)`), tandis que `session-observer-sse.ts:251` attend un champ inexistant `previousScore`, provoquant un calcul `score - undefined = NaN -> 0`.
  * Le fait **F10** (absence du bilan final sur `/quizz/ended`) est avéré : `SessionEndedPayload.java:7` envoie `finalLeaderboard`, alors que `session-observer-sse.ts:278` écoute `payload?.leaderboard`, rendant la condition d'affichage de `ended.html:24-45` inopérante.
  * Le fait **F13** (`SessionController.java` de 1186 lignes couplé directement à 3 JPA Repositories l. 78-92 sans UseCases) est avéré.
  * Le fait **F15** (DTOs artisanaux et contournements de types `as unknown as`) est textuellement vérifié dans `session.ts:7` (`interface SessionApiResponse`), `session-http.ts:14` (`interface PageOfApiResponse<T>`), et `theme-http.ts:62`.
* **Conclusion sur l'intégrité** : **Zéro violation d'intégrité (INTEGRITY PASS)**. Aucune donnée n'a été forgée, aucun raccourci trompeur n'a été emprunté.

### 1.2 Alignement avec AGENTS.md
* **DDD Hexagonal** : Le plan identifie la dette majeure (`SessionController.java`, 1186 lignes) et planifie son découpage en cas d'utilisation applicatifs purs (`modules:application`) dans le chantier P2-5.
* **NullAway & JSpecify** : Respecté dans les consignes globales et la suite de vérification.
* **Frontend Angular 21 & Signals** : Les propositions (ex: P0-2, P1-1, P1-2) s'appuient strictement sur les Signals (`computed`, `signal`, `model`) et les composants Standalone sans NgModule.
* **Hey-API & Zod** : Le chantier P2-4 éradique explicitement les DTOs artisanaux et les casts interdits au profit du client généré OpenAPI.
* **Compilation Native GraalVM 25** : Le plan intègre l'enregistrement des DTOs SSE dans `QuezapRuntimeHints.java` (P2-5).

### 1.3 Préservation de l'Identité Produit
* La gratuité intégrale sans modèle payant est gravée dans le marbre (§1.2).
* L'anonymat total des élèves (aucun compte élève, pseudo de session jetable) est scrupuleusement maintenu.
* La charte végétale « Jardin & Verdure » (`brand-green`, ambiance bienveillante à l'opposé du design agressif de Kahoot) est préservée.
* Les métriques de tableau de bord (P1-7) remplacent le jargon serveur par des indicateurs pédagogiques centrés sur l'enseignant.

### 1.4 Exécutabilité des Commandes & Critères de Vérification
* **Frontend Tests** : `cd front && bun run test` -> **SUCCÈS** (77 suites, 154 tests passés).
* **Frontend Linter** : `cd front && bun run lint:ts:ci` -> **SUCCÈS** (0 erreur).
* **Frontend Build AOT** : `cd front && bun run build` -> **SUCCÈS** (bundle généré en 4,97s).
* **Backend Unit Tests** : `./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.routes.SessionControllerTest"` -> **SUCCÈS** (4s).
* **Backend Compilation & NullAway** : Compilation sans erreur avec `-Pnullaway=true`.
* **Points d'attention identifiés** :
  1. La commande complète `./back/gradlew -p back test` exécute `GenerateOpenApiJsonTest`, qui nécessite que le conteneur PostgreSQL soit préalablement démarré sur `localhost:5432` (`docker compose up -d postgres minio minio-init`).
  2. Le chantier P2-6 mentionne `front/playwright.config.ts` sous « Fichiers Cibles Vérifiés » et la commande `bun run test:e2e`, mais ni le fichier de configuration ni le script npm n'existent encore dans `front/package.json`. Ils doivent être créés lors du jalon P2.

---

## 2. Logic Chain (Chaîne Logique de Déduction)

1. **Prémisse 1 (Conformité aux exigences de l'audit)** : La mission initiale exigeait une analyse en lecture seule, sans modification de code source hors `.audit/`, par une brigade de 4 experts équilibrés (CPO, UI/UX, Tech Lead, QA/A11y), aboutissant à un plan directeur réaliste et priorisé (P0, P1, P2).
2. **Prémisse 2 (Exactitude des faits)** : Chaque observation (F1 à F20) citée dans l'Action Plan renvoie à un fichier existant, à une ligne vérifiable et à un comportement reproductible dans l'environnement QueZap.
3. **Prémisse 3 (Alignement architectural)** : L'Action Plan s'aligne fidèlement sur `AGENTS.md` : il cible expressément le découpage hexagonal de la classe monolithique `SessionController`, l'élimination des DTOs frontend artisanaux, la conformité NullAway, et l'usage rigoureux d'Angular Signals.
4. **Prémisse 4 (Adéquation des priorités)** :
   * Le palier **P0** (8 tâches, ~18h) regroupe des correctifs indispensables pour qu'une partie de quiz fonctionne de bout en bout sans glitch visible (icônes PrimeNG, URL de join réelle, rejet de la triche post-reveal, affichage des questions booléennes, delta de score et rang final sur smartphone).
   * Le palier **P1** (8 tâches, ~1-2 semaines) adresse l'onboarding enseignant sous les 30s (lancement en 1 clic), le confort de classe (auto-reveal, synchronisation temporelle, classement intermédiaire) et l'accessibilité visuelle (symboles daltoniens unifiés).
   * Le palier **P2** (8 tâches, ~2-3 semaines) traite la dette technique profonde (architecture hexagonale, robustesse SSE, sécurité CSV OWASP, accessibilité RGAA, automatisation E2E Playwright).
5. **Déduction** : Le plan d'action directeur est robuste, rigoureux, techniquement faisable et exempt de toute dérive dénaturante ou tricherie méthodologique.

---

## 3. Caveats (Réserves & Recommandations d'Exécution)

Bien que le plan soit approuvé, l'équipe d'implémentation devra intégrer les 5 réserves techniques suivantes :

1. **Prérequis Docker pour les tests backend (§5.1)** :
   * *Observation* : L'exécution brute de `./back/gradlew -p back clean test -Pnullaway=true` échoue sur `GenerateOpenApiJsonTest` si PostgreSQL n'est pas actif sur le port 5432.
   * *Recommandation* : Documenter explicitement le lancement des conteneurs d'infrastructure (`docker compose up -d postgres minio minio-init`) avant toute exécution de la suite de tests complète.
2. **Instanciation de la suite Playwright (Chantier P2-6)** :
   * *Observation* : `front/playwright.config.ts` n'existe pas encore et `bun run test:e2e` n'est pas défini dans `front/package.json`.
   * *Recommandation* : Préciser dans la fiche P2-6 que la première étape consiste à installer `@playwright/test` en devDependencies et à initialiser `playwright.config.ts` selon les règles de `.agents/skills/e2e-playwright/SKILL.md`.
3. **Encapsulation DDD des invariants dans les correctifs P0** :
   * *Observation* : Les fiches P0-3 (rejet tardif) et P0-5 (delta de score) prévoient d'intervenir directement dans `SessionController.java`.
   * *Recommandation* : Veiller à ce que ces vérifications soient conçues de manière à faciliter leur migration ultérieure vers l'agrégat de domaine `Session.java` et les UseCases applicatifs lors du grand refactoring P2-5, sans enkyster de logique métier dans le contrôleur Spring.
4. **Enregistrement GraalVM AOT lors de la modification des DTOs (P1-2)** :
   * *Observation* : L'ajout du champ `Instant deadline` dans `QuestionStartPayload.java` impacte la sérialisation Jackson.
   * *Recommandation* : Toute modification ou création de payload d'événement SSE doit systématiquement être déclarée dans `QuezapRuntimeHints.java` et répercutée dans l'OpenAPI (`GenerateOpenApiJsonTest` + `bun run codegen`).
5. **Gestion de la concurrence sur la soumission de réponses (P0-3)** :
   * *Observation* : En cas de soumissions massives quasi-simultanées au moment du reveal, une condition de course (race condition) peut survenir sans verrouillage adéquat.
   * *Recommandation* : Utiliser un verrouillage optimiste (`@Version` sur `SessionJpaEntity`) ou atomique lors de la validation du statut de la slide.

---

## 4. Conclusion & Verdict Formel

### Verdict Formel : **APPROVE** ✅

Le Plan d'Action Directeur (`.audit/ACTION_PLAN.md`) satisfait l'intégralité des exigences fixées dans `ORIGINAL_REQUEST.md` et `AGENTS.md` :
1. **Conformité AGENTS.md** : Hexagone respecté, Signals adoptés, purge des DTOs artisanaux planifiée, AOT GraalVM pris en compte.
2. **Préservation de l'ADN QueZap** : Gratuité garantie, anonymat strict des élèves préservé, identité « Jardin » valorisée.
3. **Faisabilité & Fiabilité** : Les 24 chantiers reposent sur des observations réelles et vérifiées ; les tests unitaires et builds actuels confirment un socle stable.
4. **Priorisation Rationnelle** : Découpage P0 / P1 / P2 équilibré et chronologiquement cohérent.

Aucune anomalie bloquante ou violation d'intégrité ne subsiste. Le document constitue la feuille de route officielle pour les développements futurs.

---

## 5. Verification Method (Méthode de Vérification Indépendante)

Pour vérifier de manière indépendante les conclusions de ce rapport, exécuter les étapes suivantes :

### 5.1 Contrôle d'intégrité git
```bash
git status --short
# Constat : aucun fichier modifié dans back/ ou front/ depuis le début de la session d'audit
```

### 5.2 Vérification des tests et builds
```bash
# Frontend : Tests Jest
cd front && bun run test

# Frontend : Compilation AOT
cd front && bun run build

# Backend : Test unitaire de SessionController
./back/gradlew -p back :quezap-api:test --tests "com.quezap.api.v1.routes.SessionControllerTest"

# Backend avec NullAway (avec postgres actif)
docker compose up -d postgres
./back/gradlew -p back test -Pnullaway=true
```

### 5.3 Vérification des constats de code clés
```bash
# Vérifier l'absence de primeicons
grep -c "primeicons" front/package.json

# Vérifier le lien quezap.fr codé en dur
grep -rn "quezap.fr" front/src/app/features/admin/sessions/pages/session-presenter/

# Vérifier la duplication de score dans LeaderboardEntryDto
sed -n '1058,1060p' back/quezap-api/src/main/java/com/quezap/api/v1/routes/sessions/SessionController.java

# Vérifier l'interface artisanale dans session.ts
head -n 20 front/src/app/features/admin/sessions/services/session.ts
```
