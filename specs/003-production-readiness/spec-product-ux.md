# Spécification Produit, UX/UI & Qualité Frontend — QueZap Production-Ready

**Version** : 1.0.0  
**Date** : 2026-11-18  
**Branche auditée** : `goal/production-ready` (`HEAD 2b0a5d4` + WIP non commité sur `front/`)  
**Rôle** : Lead Product & UX/UI Auditor / Spécificateur Frontend (Conseil de Décision)  
**Périmètre** : Ensemble du frontend Angular 21 (`front/`), cohérence contrats OpenAPI/Hey-API/Zod, PWA, accessibilité (RGAA/WCAG AA), ergonomie mobile (`390×844`) et vidéoprojecteur (`1920×1080` et `1280×720`).

---

## 1. Synthèse Exécutive & État Baseline Observé

### 1.1 Vérifications Techniques Actuelles (`front/`)
Exécution directe sur l'état actuel (`HEAD 2b0a5d4` + WIP upload images) :
- **`bun run lint`** (`ng lint` + `stylelint --max-warnings 0 "src/**/*.css"`) : **PASS** (0 erreur, 0 warning).
- **`bun run test`** (`jest --silent`) : **PASS** — `82` suites passées / `82`, `239` tests passés / `239`.  
  *Alerte observée* : Jest émet `A worker process has failed to exit gracefully and has been force exited` (fuite de timers/handles non nettoyés dans certains tests).
- **`bun run build`** (`ng build` + `ngsw-config.js`) : **PASS** en `5.778s` (`Initial total: 1.22 MB / 271.13 kB gzip`).  
  *Warning observé* : dépendance CommonJS `qrcode` utilisée par `angularx-qrcode` (`optimization bailout`).

### 1.2 Progrès déjà acquis depuis le rapport d'audit initial (`12 Oct 2026`)
De nombreux chantiers de `.audit/RAPPORT_AUDIT_GLOBAL_QUEZAP.md` et des specs `001` / `002` ont déjà été intégrés dans le code :
- Installation de `primeicons` (`^8.0.2`) et import dans `front/src/styles/index.css`.
- Dynamisation de l'URL de join (`displayJoinUrl`, `joinUrl`) et modal QR zoomable dans `SessionPresenter`.
- Unification des formes géométriques SVG (`quizz-question-icon` : Cercle, Losange, Croix, Hexagone) + triple codage (numéro + forme + couleur) entre `SessionPresenter` et `QuestionAnswer`.
- Attribut `<html lang="fr">`, skip-link `#main-content`, régions `aria-live` sur `QuizzRunner` et `SessionPresenter`, `@media (prefers-reduced-motion: reduce)`.
- Question `CASH` + `TextNormalizer` (`normalizeText` / `matchesTolerant`), mode `MANUAL` / `AUTOMATIC`, `isPaused`, `SessionReport` + export CSV, écran de fin `/quizz/ended` avec classement complet, et début de migration d'upload d'images stagées (WIP).

### 1.3 Verdict Produit Actuel : **Non Prêt pour la Production**
L'audit ligne par ligne de **toutes les routes, composants, stores, services et contrats API** révèle encore **7 bugs bloquants (P0)** — dont plusieurs formulaires physiquement inutilisables, un blocage de démarrage applicatif sur token périmé, une pagination cassée côté serveur sur toutes les listes admin, et des pertes silencieuses de données — ainsi que **17 défauts P1** et **12 dettes P2** détaillés ci-dessous.

---

## 2. Audit Détaillé du WIP Non Commité (Upload Images & API Client)

Fichiers inspectés : `front/openapi.json`, `front/src/app/core/api-client/{index.ts, sdk.gen.ts, types.gen.ts, zod.gen.ts}`, `front/src/app/core/services/picture-upload/{picture-upload.ts, picture-upload.spec.ts}`, `question-editor.{ts, html, spec.ts}`, `answer.{ts, html, spec.ts}`, `editor-store.spec.ts`, `front/nginx.conf`.

### 2.1 Cohérence `openapi.json` ↔ Client Hey-API (`sdk.gen.ts`, `types.gen.ts`, `zod.gen.ts`)
- **Endpoint `/apiv1/pictures/upload` présent et régénéré** dans `openapi.json`, `sdk.gen.ts` (`upload`), `types.gen.ts` (`UploadData`, `UploadResponses`, `PictureUploadResponseDto`) et `zod.gen.ts` (`zPictureUploadResponseDto`, `zUploadBody`, `zUploadResponse`).
- **Défaut #WIP-1 (P1) — Quirk Zod sur `multipart/form-data`** : dans `zod.gen.ts` (l.136-138), `zUploadBody` génère `file: z.string()` pour `format: binary`. Ce validateur ne doit **jamais** être utilisé tel quel sur un `File` JS (il échouerait sur `instanceof File`).
- **Défaut #WIP-2 (P2) — Cast redondant dans `PictureUploadService`** (`picture-upload.ts` l.31) : `const dto = data as PictureUploadResponseDto` est superflu après `if (error || !data)`, et `dto.pictureUrl as PictureUrl` masque l'absence de validation Zod de `zPictureUploadResponseDto`.

### 2.2 Bugs & Lacunes UX sur le Flow d'Upload (`PictureUploadService`, `QuestionEditor`, `AnswerComponent`)
1. **BUG #WIP-3 (P0) — Échecs d'upload totalement silencieux** :
   - Dans `question-editor.ts` (l.83-97) et `answer.ts` (l.91-104) :
     ```ts
     next: (output) => {
       this.isUploading.set(false)
       if (isSuccess(output)) { ... }
     }
     ```
     Lorsque `PictureUploadService.upload(file)` retourne `{ kind: 'failure' }` (fichier trop lourd 413, format rejeté 400/415, session expirée 401, erreur 500), **aucun toast ni message d'erreur n'est affiché**. L'utilisateur voit juste le spinner disparaître sans explication.
   - `PictureUploadService` (l.24-28) ignore le message d'erreur HTTP (`error`) et renvoie systématiquement le générique `'Échec du téléversement de l’image'`.
2. **BUG #WIP-4 (P0) — Désalignement MIME `accept="image/*"` vs Backend** :
   - `PictureMetadataValidatorService.java` (l.14-15) n'autorise strictement que `image/jpeg`, `image/png`, `image/webp`.
   - `question-editor.html` (l.23) et `answer.html` (l.30) utilisent `accept="image/*"` sans filtre d'extension ni validation client préalable, laissant l'utilisateur choisir des `.gif`, `.svg`, `.avif` ou `.bmp` qui échouent silencieusement côté API.
3. **BUG #WIP-5 (P1) — Fuites RxJS & Double-upload / État `p-fileupload`** :
   - `this.pictureUploadService.upload(file).subscribe(...)` dans `question-editor.ts` (l.83) et `answer.ts` (l.91) n'utilise pas `takeUntilDestroyed(this.destroyRef)`. Si l'utilisateur ferme l'éditeur pendant un upload lent, le callback tente de muter un store/composant détruit.
   - Aucune validation client explicite de taille (`maxFileSize="5000000"` sur `p-fileUpload` n'affiche aucun toast/erreur lorsque PrimeNG rejette un fichier > 5 Mo).
4. **BUG #WIP-6 (P1) — Accessibilité & Contrôle `Boolean` dans `answer.html`** :
   - `answer.html` (l.29) utilise `chooseLabel=" "` (espace vide) **sans `ariaLabel`**, rendant le bouton d'upload d'image de réponse muet pour les lecteurs d'écran.
   - Sur une question de type `Vrai / Faux` (`Boolean`), `readonly()` désactive le texte mais **n'empêche pas d'afficher le bouton d'upload d'image sur Vrai/Faux**, alors que `TypeSelector` efface les images de réponse en `Boolean` et `SessionController` ne les projette jamais (`new SafeAnswerDto(0, "Vrai", null)`).
5. **Observation #WIP-7 (P2) — Images orphelines & `nginx.conf`** :
   - `removePicture()` (`question-editor.ts` l.101, `answer.ts` l.108) supprime l'URL localement sans prévenir le serveur si c'était une image stagée non encore claimée (le nettoyage repose donc sur le TTL backend des `staged_pictures`, à spécifier côté backend).
   - `front/nginx.conf` (l.5) contient encore un commentaire obsolète `# Allow large payloads (e.g. quiz saves with multiple embedded base64 images)` alors que les `data:` URLs sont désormais interdites (`QuezapController` l.587). Le proxy `location ^~ /quezap-public-images/` (l.86) utilise bien `^~`, prioritaire sur la regex `~* \.(?:png|jpg|...)$` (l.54), ce qui évite un 404 nginx sur les images MinIO.
   - `front/proxy.conf.json` redirige correctement `/quezap-public-images` vers `http://localhost:9190` en développement.

---

## 3. Audit Complet par Parcours Utilisateur (P0 / P1 / P2)

Légende : **🔴 P0** = Bloquant produit / parcours cassé / perte de données ; **🟡 P1** = Irritant majeur / trou de parcours / incohérence UX ; **🟢 P2** = Dette technique, A11y fine, optimisation.

### 3.1 Parcours Authentification, Bootstrap & Compte Enseignant

| ID | Criticité | Constat & Preuve exacte dans le code | Impact Utilisateur |
| :--- | :---: | :--- | :--- |
| **UX-AUTH-01** | 🔴 **P0** | **`AppInitializer` + `AuthenticatedUserStore.refresh()` bloquent tout le démarrage** (`app.initializer.ts` l.28-46, `authenticated-user.ts` l.50-89 & 159-166). Lors du boot avec des tokens expirés en `localStorage`, `loadInitialState()` met les tokens dans `_temporaryTokens` (`_authenticated` reste `undefined`) puis appelle `me()`. Sur `401`, `refreshSessionInterceptor` appelle `store.refresh()`, qui échoue immédiatement (`const currentAuth = store._authenticated?.(); if (!currentAuth) return throwError(...)`). Le rejet remonte dans `forkJoin` d'`AppInitializer` et déclenche un `window.confirm(...)` bloquant **même pour un élève** rejoignant `/quizz/join`. | Application inutilisable après expiration de session tant que l'utilisateur ne vide pas manuellement son `localStorage`. |
| **UX-AUTH-02** | 🔴 **P0** | **`RegisterModal` et `ForgottenPassword` : `[formField]` manquant sur les inputs** (`register-modal.html` l.35-36, `forgotten-password.html` l.42-43). Les deux templates posent `quizzFieldError [fieldState]="..."` **sans `[formField]="..."`**. La saisie clavier dans le champ email ne met jamais à jour le Signal Form (`disabled` reste `true` à l'infini). | Impossibilité totale de créer un compte via l'accueil ou de soumettre "Mot de passe oublié". |
| **UX-AUTH-03** | 🔴 **P0** | **Payload `forgotten-password` incompatible avec l'API** (`authentication-http.ts` l.140). Envoie `body: { email }` alors que `ForgottenPasswordRequest` (`openapi.json`, `types.gen.ts` l.232) exige `{ identifier: string }`. | Requête `POST /apiv1/auth/forgotten-password` rejetée en `400 Bad Request`. |
| **UX-AUTH-04** | 🟡 **P1** | **`refreshSessionInterceptor` sans contexte `IS_REFRESHING_ACCESS_TOKEN` ni verrou concurrent** (`authentication-http.ts` l.54-86, `refresh-session-interceptor.ts` l.30-51). `AuthenticationHttpService.refresh()` ne passe pas le `HttpContext` avec `IS_REFRESHING_ACCESS_TOKEN = true` (jeton défini mais inutilisé partout) : si `/apiv1/auth/refresh` renvoie `401`, l'intercepteur boucle sur lui-même. De plus, 2 requêtes parallèles expirées déclenchent 2 refresh concurrents. | Boucle d'erreur sur expiration de refresh token et déconnexions aléatoires au chargement du dashboard (`quezaps` + `sessions` en parallèle). |
| **UX-AUTH-05** | 🟡 **P1** | **Erreur d'identifiants mal affichée sur `/auth/login`** (`login.ts` l.93-106). `AuthenticationHttpService.login` retourne `UnauthorizedError` (l.31), mais `login.ts` teste `if (err instanceof ValidationError)` pour activer `invalidCredentials`. Résultat : un mauvais mot de passe affiche l'erreur technique générique + toast `GlobalErrorHandler` et efface tout le champ. De plus, `login.html` n'a pas `quizzFieldError`, comporte la faute `"rénitialiser"` (l.75), et désactive le bouton sur `!dirty()` (problème avec l'autofill navigateur). | Feedback trompeur lors d'une faute de frappe de mot de passe ; blocage possible avec gestionnaire de mots de passe. |
| **UX-AUTH-06** | 🟡 **P1** | **`ResetPassword` & `ResetPasswordForm` : validation token `zod.jwt()` et `autocomplete`** (`reset-password.ts` l.35, `reset-password-form.html` l.42). `zod.jwt()` rejette les tokens opaques/UUID si le backend n'émet pas un JWT pour le reset ; `password-confirmation` porte `autocomplete="current-password"` au lieu de `"new-password"`. Note produit : les endpoints backend `forgotten-password`, `verify-reset-token`, `reset-password`, `activate`, `POST /users` sans email réel sont encore incomplets (à aligner avec `@spec-architect`). | Lien de réinitialisation rejeté côté front ; mauvais remplissage par les navigateurs. |

---

### 3.2 Parcours Landing & Élève Mobile (`390×844` — Join, Lobby, QuizzRunner, Ended, Expired)

| ID | Criticité | Constat & Preuve exacte dans le code | Impact Utilisateur |
| :--- | :---: | :--- | :--- |
| **UX-PLAY-01** | 🔴 **P0** | **Vote optimiste sans gestion d'erreur serveur** (`quizz-question.ts` l.96, `binary-question.ts` l.122, `boolean-question.ts` l.134, `cash-question.ts` l.92, `session-api-http.ts` l.144-174). Chaque composant met à jour `mySubmittedChoice` et affiche le bandeau vert `"✓ Choix enregistré !"` **avant** la réponse HTTP, puis appelle `.subscribe()` sans vérifier le retour. Si l'API renvoie `409 Conflict` (réponse tardive post-reveal), `400` (chrono écoulé), `403` (exclu) ou coupe réseau, l'élève croit à tort que son vote compte. | Perte silencieuse de points ; incompréhension totale en classe. |
| **UX-PLAY-02** | 🔴 **P0** | **Reconnexion après fermeture d'onglet perd la réponse en cours et le bilan final** (`active-session-persistence.ts` l.105, 154, 200, 241). `saveSubmittedChoice` et `saveFinalResults` utilisent `sessionStorageRef` au lieu de `localStorageRef`. Fermer l'onglet puis rouvrir l'URL (scénario `specs/002` US-1) efface `mySubmittedChoice` et `finalResults`. De plus, `hasActiveSessionGuard.isSessionActive` (`has-active-session-guard.ts` l.50) écrit `session.endedAt === null || session.startedAt !== null` : le `||` laisse passer une session **déjà terminée** (`startedAt !== null && endedAt !== null`) dans `/quizz` ou `/quizz/lobby` ! | En cas de fermeture accidentelle d'onglet, l'élève ne retrouve ni son vote soumis ni ses résultats de fin ; une vieille session terminée reste "active" dans le guard. |
| **UX-PLAY-03** | 🟡 **P1** | **Session en pause n'empêche pas de voter** (`quizz-question.html` l.34, `binary-question.html` l.36, `boolean-question.html` l.36, `cash-question.ts` l.48). `[inert]="cannotAnswerAnymore()"` ne vérifie que `timeLeft() === 0`, ignorant `isSessionPaused()`. | Pendant `"EN PAUSE — Écoutez le professeur"`, les élèves peuvent continuer à cliquer ou modifier leurs réponses. |
| **UX-PLAY-04** | 🟡 **P1** | **Encombrement vertical sur mobile `390×844` avec image** (`quizz-question.html` l.24, `boolean-question.html` l.24, `binary-question.html` l.24, `cash-question.html` l.24, `picture.html` l.8). Un label visible `<span id="question-image-desc" class="ms-4 text-xl select-none">Image de la question</span>` est affiché sous chaque image (au lieu d'être `sr-only` comme pour les réponses), et l'image monte jusqu'à `min(42vh, 360px)`. Avec 4 tuiles de réponse + bandeau timer + footer chrono, les choix 3 et 4 sortent de l'écran `390×844`. | Nécessite de scroller sur smartphone pour voter sur une question illustrée ; texte parasite `"Image de la question"`. |
| **UX-PLAY-05** | 🟡 **P1** | **Résilience SSE côté élève incomplète** (`session-observer-sse.ts` l.171-173, `active-session.ts` l.251). `eventSource.onerror` ne fait qu'un `console.debug(...)` et ne recrée pas l'`EventSource` lorsque `readyState === EventSource.CLOSED` (coupure 4G prolongée / redémarrage nginx). En outre, `ActiveSessionStore` vérifie `sessionObserver instanceof SessionObserverSseService`, couplant le store au transport concret. | Un élève subissant une coupure réseau de >30s reste bloqué sur l'écran courant sans jamais recevoir les événements suivants. |
| **UX-PLAY-06** | 🟡 **P1** | **Incohérence QCM 2 réponses et QCM choix multiple** (`quizz-question.ts` l.59, `quizz-question.html` l.45, `SessionController.java`). L'éditeur permet des `Quizz` à **2, 3 ou 4** réponses et plusieurs réponses cochées `isCorrect`, alors que `QuizzQuestionView` valide `.refine(data => Object.keys(data).length >= 3 && Object.keys(data).length <= 4)` et force `[radio]="true"` (choix unique), tout comme le backend (`SubmitAnswerRequest` ne prend qu'un `answerIndex` unique). | Un quiz `Quizz` à 2 réponses échoue la validation Zod interne côté élève ; ambiguïté produit sur le multi-réponses. |
| **UX-PLAY-07** | 🟡 **P1** | **Affichage correction mobile si non-votant + Page `/quizz/ended` après reload** (`quizz-runner.html` l.101-113, `ended.ts`). Quand `mobileCorrectionAllowed` est activé et l'élève n'a pas voté (`choice == undefined`), la carte affiche `"❌ Réponse incorrecte"` au lieu de `"⏱️ Aucune réponse envoyée"`. Sur `/quizz/ended`, un rechargement après perte de `sessionStorage` retombe sur un formulaire PIN vide sans tenter de récupérer `/apiv1/sessions/{id}/scores`. | Message injuste pour un élève sans vote ; perte du classement final au rafraîchissement sur mobile. |
| **UX-PLAY-08** | 🟡 **P1** | **Landing (`pages/home/home.html`) : `<h1>` masqué et CTA ambigu si connecté** (`home.html` l.12, 46-62, 73). `<h1 class="hidden">` rend le premier titre absent de l'arbre d'accessibilité (`<h2>` devient premier titre). Le bouton `"Créer une partie"` ouvre systématiquement `<quizz-register-modal>` même si l'enseignant est déjà connecté (devrait rediriger vers `/admin/sessions/create` ou `/admin`). Faute `"tu ... saisis"` -> `"vous ... saisissez"` ou `"le public ... saisit"` (l.73). | Un enseignant déjà authentifié retombe sur la modale d'inscription ; rupture hiérarchie de titres H1→H2 RGAA. |
| **UX-PLAY-09** | 🟢 **P2** | **Validation pseudo `<>` absente côté front & Code mort dans `QuizzRunner`** (`nickname-chooser.ts` l.52-56, `quizz-runner.ts` l.72-77). Le backend rejette `<` et `>` (`^[^<>]*$`), non validé dans `nicknameForm`. `QuizzRunner` conserve les signaux inutilisés `myRanking` et `pointsEarned`. | Aller-retour réseau inutile si un élève saisit `<` ou `>` dans son pseudo. |

---

### 3.3 Parcours Enseignant Admin (`Dashboard`, `Themes`, `Mes Quezaps`, `Éditeur`)

| ID | Criticité | Constat & Preuve exacte dans le code | Impact Utilisateur |
| :--- | :---: | :--- | :--- |
| **UX-ADM-01** | 🔴 **P0** | **Pagination cassée sur TOUTES les listes Admin (`Themes`, `Quezaps`, `Sessions`)** (`theme-http.ts` l.23, `quezap-http.ts` l.39, `session-http.ts` l.31 vs `PaginationMapper.java` l.20-34). Les 3 services envoient le query param `pageSize` au lieu de `perPage` attendu par `PaginationDto`. Sur le backend, `dto.perPage()` est toujours `null`, donc `PaginationMapper.toDomain()` tombe systématiquement sur `Pagination.firstPage()`. | Impossible de changer de page (page 2, 3...) ou de modifier le nombre d'éléments par page sur Thèmes, Mes Quiz et Sessions. |
| **UX-ADM-02** | 🔴 **P0** | **`TitleEditor` n'affiche jamais le titre d'un QueZap existant** (`title-editor.ts` l.34-39, `title-editor.html` l.15). Contrairement à `PhraseEditor` (qui synchronise `editedPhrase` via `effect`), `TitleEditor` initialise `editedTitle` à `''` et ne le met à jour que dans `openCallback()` (au clic). En édition (`/admin/quezaps/:id/edit`), la barre d'en-tête affiche le placeholder `"Titre du QueZap (cliquez pour éditer)..."` au lieu du titre réel. | L'enseignant croit que le titre de son quiz a été effacé. |
| **UX-ADM-03** | 🟡 **P1** | **Chiffres factices codés en dur sur le Dashboard Enseignant** (`admin/dashboard/home/home.ts` l.65-66, `home.html` l.134). `this.totalParticipants.set(count > 0 ? count * 24 : 0)` et `this.averageSuccessRate.set(count > 0 ? 78 : 0)` affichent des statistiques inventées. De plus, `home.html` l.134 contient deux attributs `class` sur le même `<i>` (`class="pi" class="text-xs"`), invalidant l'un des deux styles. | Trompe l'enseignant avec de fausses métriques de classe ; icône de lancement dégradée. |
| **UX-ADM-04** | 🟡 **P1** | **Barème `0 pt` écrasé à `1000` lors de la sauvegarde et du rechargement** (`quezap-http.ts` l.112, 262). L'opérateur `|| 1000` au lieu de `?? 1000` transforme `0` ("Sans points (0 pt)") en `1000`. | Une question configurée à 0 point (sondage/brise-glace) redevient notée sur 1000 pts après sauvegarde. |
| **UX-ADM-05** | 🟡 **P1** | **Contrôles factices ou incohérents dans l'Éditeur de QueZap** (`question-editor.ts` l.44-45, `limit-selector.ts` l.39, `quezap-http.ts` l.168, 251). (a) `mediaFormat` (`STANDARD`/`LARGE`/`FULLSCREEN`) et `backgroundTheme` (`CLASSIC`/`DARK`/`CLEAN`) sont des signaux locaux non sauvegardés, perdus au changement de question et ignorés en projection. (b) `"Pas de limite"` (`0`) dans `LimitSelector` est interdit par le backend (`@Min(5) @Max(300)` sur `QuezapSlideRequest`) et silencieux (`timer: q.limit?.seconds \|\| 30` sauve 30s). (c) Aucun sélecteur de **Thème** ni champ **Explication** ni visibilité **`isPublic`** dans l'éditeur (toutes les questions partent sans `themeId` -> thème `"Général"`, `isPublic: true` forcé, `explanation` impossible à saisir). | Frustration enseignant : options visibles sans effet réel ; impossible de catégoriser ses questions ou de rédiger l'explication projetée au reveal. |
| **UX-ADM-06** | 🟡 **P1** | **Barre latérale gauche & Ergonomie Éditeur (`specs/002` US-4)** (`quezap-editor.html` l.50, `question-list-view.html` l.8-14, `question-action-bar.html` l.1). Sur mobile (`< md`), la liste des questions est figée à `h-56` (scroll forcé dès 2 questions). Sur desktop, la colonne `w-13 shrink-0` réservée à `quizz-question-action-bar` reste vide et bloque 52px de largeur pour chaque carte non sélectionnée, rétrécissant les aperçus et provoquant des retours à la ligne / scroll prématuré. | Perte d'espace latéral dans la sidebar et ascenseur inutile sur mobile. |
| **UX-ADM-07** | 🟡 **P1** | **Erreurs avalées à la sauvegarde / lancement dans `QuezapEditor`** (`quezap-editor.ts` l.112, 152, `type-selector.html` l.2, `answer-editor.html` l.15). (a) `onSaveQuezap()` fait `throw new Error('...', err)` (2e arg invalide) sans message toast explicite. (b) `onLaunchSession()` fait `.catch(() => void 0)` : si `persist()` échoue en validation, rien ne s'affiche. (c) `type-selector.html` l.2 affiche le caractère cassé `⯑`. (d) Le bloc `Cash` dans `answer-editor.html` impose un fond sombre `bg-slate-900/40 text-white` peu cohérent avec le thème clair par défaut. | L'enseignant clique sur "Lancer en direct" avec une question invalide sans comprendre pourquoi rien ne se passe. |
| **UX-ADM-08** | 🟡 **P1** | **`QuezapCard` & `ThemeCard` : thème figé `'Général'`, souscriptions sans `takeUntilDestroyed` et cohérence de route de lancement** (`quezap-card.ts` l.63, 72, 94, 123, `quezap-http.ts` l.61-64). `QuezapSummaryDto` ne fournit pas le thème, donc toutes les cartes affichent `"Général"`. `QuezapCard` lance `/admin/sessions/:id/present` tandis que `QuezapEditor` et `SessionCard` lancent `/projector/:id`. | Incohérence d'URL de projection et risque de fuite mémoire sur navigation rapide. |

---

### 3.4 Parcours Sessions, Projection Vidéoprojecteur (`1920×1080` / `1280×720`) & Rapports

| ID | Criticité | Constat & Preuve exacte dans le code | Impact Utilisateur |
| :--- | :---: | :--- | :--- |
| **UX-PRES-01** | 🔴 **P0** | **Routes `/projector/:id` et `/present/:id` non protégées & erreurs API présentateur ignorées** (`features/routes.ts` l.44-55, `session-presenter.ts` l.267-300, 781-834). Si l'enseignant ouvre `/projector/:id` sans session auth active (ou token expiré), `startSession()`, `revealAnswers()`, `nextStep()`, `pauseSession()`, `updateSettings()`, `kickParticipant()` échouent **silencieusement** (aucun check `output.kind === 'failure'` ni toast ni redirection login). | En classe, cliquer sur "Démarrer" ou "Révéler" peut ne rien faire sans aucun message d'erreur. |
| **UX-PRES-02** | 🟡 **P1** | **Réglages `mobileCorrectionEnabled` et `explanationDurationSeconds` absents de l'UI Présentateur** (`session-presenter.ts` l.143-168, `session-presenter.html` l.25-73). Bien que le backend et `SessionHttpService.updateSettings` gèrent `mobileCorrectionEnabled` (`specs/001` FR-005b) et `explanationDurationSeconds` (`specs/001` FR-002), aucun bouton/toggle n'existe dans la barre du présentateur pour activer/désactiver la correction sur mobile ou ajuster les 15s d'explication. De plus, `setProgressionMode` et `setExplanationDuration` n'envoient pas `mobileCorrectionEnabled`. | Impossibilité pour l'enseignant de régler la durée d'explication ou d'autoriser le feedback sur smartphone comme spécifié dans `001`. |
| **UX-PRES-03** | 🟡 **P1** | **Dérive du timer présentateur & flash d'état au rechargement** (`session-presenter.ts` l.291-293, 441-456, 638-641). (a) `SessionPresenter` ignore `payload.deadline` (déjà reçu dans `QUESTION_START`) et décrémente localement toutes les 1000ms : dérive avec le serveur et les mobiles. (b) Au reload sur une session `STARTED`, `ngOnInit` passe immédiatement à `stage.set('QUESTION')` avant l'arrivée du SSE `QUESTION_START`/`QUESTION_REVEAL`, affichant brièvement `Question 1 / 1` vide avec chrono `30`. (c) Reload pendant `REVEAL` en mode `AUTOMATIC` réinitialise le décompte d'explication à `15s` complètes. | Décalage chrono entre tableau et mobiles ; flash visuel disgracieux si F5 en pleine séance. |
| **UX-PRES-04** | 🟡 **P1** | **Bugs visuels de mise en page sur `SessionPresenter` (`QUESTION`, `REVEAL`, `PODIUM`, `LOBBY`)** : (a) **Collision Cash + Image** (`session-presenter.html` l.280-310) : en question `CASH` avec `pictureUrl`, le wrapper image porte `h-full`, écrasant la carte `"Mode Cash — Saisie Libre"` hors du conteneur `overflow-hidden`. (b) **Image absente au Reveal** (`session-presenter.html` l.380-462) : `currentQuestion().pictureUrl` disparaît dès `stage === 'REVEAL'`, empêchant de commenter une carte/schéma pendant les explications. (c) **Double `[class]` sur carte Reveal** (`session-presenter.html` l.404-408) : `[class]="getChoiceColorClass(i)"` combiné à `class="..."` et `[class.opacity-40]` peut écraser les classes statiques selon l'ordre d'application Angular. (d) **Bouton d'exclusion invisible sur tactile** (`session-presenter.html` l.170) : `opacity-0 group-hover:opacity-100` masque la croix d'exclusion sur TNI/tablette. (e) **Podium 720p & Retour Bilan** : aucune passerelle depuis le Podium vers `/admin/sessions/:id/report` ou `/admin/sessions` ; en `1280×720`, la somme des marges verticales (`my-6`, `p-5`) risque de couper le bouton CSV dans `max-h-screen overflow-hidden`. | Illustration disparue pendant les explications ; crash visuel sur Cash illustré ; exclusion difficile sur écran tactile. |
| **UX-PRES-05** | 🟡 **P1** | **Création/Édition de session imparfaite** (`session-create.ts` l.77-96, `session-create.html`, `session-card.html` l.63-72, `session-edit.ts`). (a) `SessionCreate` ne retourne pas la Promise dans `submit()`, cassant l'état `submitting()`, n'affiche pas `quizzFieldError`, limite à 100 quiz, et renvoie vers la liste au lieu de proposer "Créer et projeter". (b) `SessionCard` affiche un bouton crayon "Modifier la session" menant à `SessionEdit` qui dit simplement *"Une session ne peut pas être modifiée une fois créée"*. | Parcours inutile menant à un cul-de-sac ("page morte"). |

---

## 4. Audit Technique Transversal : Conformité `AGENTS.md`, PWA, A11y & Responsive

### 4.1 Violations `AGENTS.md` (Hey-API, Zod, Typings, RxJS)

| Règle `AGENTS.md` | État Constaté & Fichiers Impactés | Action Requise |
| :--- | :--- | :--- |
| **Zéro `any` / `$any()`** | **CONFORME** (0 occurrence dans `front/src/` hors `core/api-client/` généré, confirmé par `rg` et ESLint `no-explicit-any`). | Maintenir strictement. |
| **Utiliser `sdk.gen.ts` ou `client.get<{ 200: Dto }>`; JAMAIS `client.post<Dto>`** | **NON CONFORME** : `activation-account-http.ts` l.18 utilise `client.post<MessageResponseDto>` (retourne l'union des champs !). Tous les services HTTP (`auth-http`, `theme-http`, `quezap-http`, `session-http`, `session-api-http`, `register-http`, `activation-account-http`) appellent `client.*` en dur avec URL string au lieu des fonctions `sdk.gen.ts` (`login`, `me`, `refresh`, `forgottenPassword`, `find`, `add`, `list1`, `create1`, `list2`, `getById`, `update`, `create2`, `exportCsv`, etc.). C'est cette entorse qui a masqué les bugs `ForgottenPasswordRequest` (`{ email }` vs `{ identifier }`), `PaginationDto` (`pageSize` vs `perPage`) et `SessionCreatedDto`. | Migrer tous les services HTTP vers les fonctions typées `sdk.gen.ts` + validation Zod (`zod.gen.ts`) sur les formulaires/réponses critiques. |
| **Interdiction d'appels `HttpClient` contournant l'ACL dans composants/pages** | **NON CONFORME** : `SessionPresenter` (`session-presenter.ts` l.116, 793) et `SessionReport` (`session-report.ts` l.36, 75) injectent directement `HttpClient` pour `export/csv`. (`QuestionIcon` et `MarkdownViewer` l'utilisent pour des assets statiques locaux, acceptable mais à isoler dans un loader dédié pour `QuestionIcon`). | Déplacer `exportCsv(id)` dans `SessionService` (`SessionHttpService` via `sdk.gen.ts`) avec gestion d'erreur propre. |
| **DTOs / interfaces manuelles** | **PARTIELLEMENT NON CONFORME** : `SessionPresenter` (l.23-103) et `SessionObserverSseService` (l.23-83) redéfinissent des interfaces SSE (`SseEnvelope`, `SsePayload`, `IntermediateLeaderboardEntry`) et `SessionHttpService.createSession` ment sur son type retour (`SessionDetailDto` au lieu de `SessionCreatedDto`). | Exporter des contrats SSE unifiés (partagés avec les schémas OpenAPI/backend si exposés) et aligner `createSession` sur `SessionCreatedDto`. |
| **`takeUntilDestroyed(destroyRef)` sur toute souscription composant** | **NON CONFORME** : manquant dans `quezap-card.ts` (3 endroits), `theme-card.ts`, `question-editor.ts`, `answer.ts`, `quizz-question.ts`, `binary-question.ts`, `boolean-question.ts`, `cash-question.ts`, `logout-button.ts`, `active-session.ts` (`leaveSession`), `register-modal.ts` (`firstValueFrom` OK mais préférer pipe détruit), `theme-editor.ts`. | Ajouter `takeUntilDestroyed(this.destroyRef)` systématiquement sur toutes les souscriptions RxJS déclenchées en composant. |
| **Contournements `as unknown as` / `as never`** | `editor-store.spec.ts` l.159 (`as never`), specs de tests avec `as unknown as Event` / `SsePayload`. | Limiter aux mocks de tests unitaires strictement typés. |

### 4.2 PWA (`ngsw-config.json` & `manifest.webmanifest`)
- **`navigationUrls` manquant dans `front/ngsw-config.json`** : sans exclusion explicite `["/**", "!/**/*.*", "!/apiv1/**", "!/quezap-public-images/**"]`, une navigation directe (ex. ouverture d'un lien `/apiv1/sessions/.../export/csv` ou d'une image `/quezap-public-images/...` sans extension) peut être interceptée par le Service Worker et renvoyer `index.html`.
- **Cache des thèmes `/apiv1/themes/**` (`strategy: "performance"`, `maxAge: "1d"`)** : en mode `performance`, après création/modification/suppression d'un thème dans `/admin/themes`, un enseignant en PWA risque de recevoir l'ancienne liste cachée pendant 1 jour lors du rechargement ! Passer les endpoints mutables `/apiv1/themes/**` en `freshness` (ou supprimer ce dataGroup / le limiter à `/apiv1/config/**`).
- **Double manifeste (`manifest.webmanifest` et `site.webmanifest`)** : fichiers quasiment identiques (`1857` vs `1856` octets) dans `front/public/`. Conserver `manifest.webmanifest` comme référence unique.
- **SSE bypass** : `?ngsw-bypass=true` est bien présent sur `EventSource` dans `SessionPresenter` (l.528) et `SessionObserverSseService` (l.137).

### 4.3 Accessibilité (RGAA / WCAG 2.1 AA) & Responsive (`390×844`, `1280×720`, `1920×1080`)
1. **Conflit critique Focus Trap (`app.html` l.67 + `app.config.ts` l.94)** :
   `<main pFocusTrap [pFocusTrapDisabled]="asWebsite()">` emprisonne le focus clavier dans `<main>` dès que `asWebsite() === false` (`Lobby`, `QuizzRunner`, `QuezapEditor`, `SessionPresenter`), alors que `providePrimeNG({ overlayAppendTo: 'body' })` monte les overlays (`p-select`, `p-confirmpopup`, `p-confirmdialog`, `p-image` preview) dans `<body>` **hors de `<main>`**. Conséquence : navigation clavier cassée sur les modales de confirmation et dropdowns de l'éditeur. **Solution** : retirer `pFocusTrap` de `<main>` (qui n'est pas une modale !) et laisser PrimeNG gérer le focus trap des vraies modales (`p-dialog`, `p-confirmdialog`).
2. **Scrollbars forcées sur `quizz-root`** (`front/src/styles/index.css` l.14) : `overflow-scroll` affiche des barres de défilement permanentes horizontale et verticale même sur les vues `h-dvh` (`SessionPresenter`, `QuezapEditor`). Remplacer par `overflow-x-hidden overflow-y-auto` (et `overflow-hidden` quand `asWebsite() === false` si géré par la vue).
3. **Sémantique HTML & ARIA** :
   - `pages/home/home.html` : remplacer `<h1 class="hidden">` par `<h1 class="sr-only">` (ou un `<h1>` visible) pour restaurer la hiérarchie H1 → H2.
   - `question-list-view.html` (l.5) : remplacer `role="menu"` (sans `menuitem`) par `role="listbox"` / `role="option"` (avec `aria-selected`) ou une liste sémantique `<ul>` / `<li>`.
   - `answer.html` (l.29) : ajouter `ariaLabel="Ajouter une image à la réponse"` sur `<p-fileUpload>` et supprimer `chooseLabel=" "`.
   - `session-presenter.html` (l.170) : rendre le bouton d'exclusion joueur visible sans hover sur écrans tactiles (`opacity-70 md:opacity-0 md:group-hover:opacity-100`).
4. **Contraste & Daltonisme** :
   - Triple codage validé sur `QuestionAnswer` et `SessionPresenter` (Numéro + Forme SVG `Circle/Diamond/Cross/Hexagon` + Couleur).
   - Sur `SessionReport` (`session-report.html` l.172-182), ajouter les variantes `dark:bg-amber-950/80 dark:text-amber-300` et `dark:bg-rose-950/80 dark:text-rose-300` manquantes (seul `emerald` possède ses classes `dark:`).
   - Vérifier sur `390×844` que les 4 options QCM + image question (réduire `max-height` mobile à `min(28vh, 220px)` et passer `#question-image-desc` en `sr-only`) tiennent confortablement sans scroll excessif.

---

## 5. Architecture Cible Frontend & Lots d'Exécution Sans Collision

### 5.1 Principes Directeurs de Correction
1. **Zéro appel HTTP artisanal** : tous les appels REST passent par les fonctions typées `sdk.gen.ts`, y compris `exportCsv` et `upload`.
2. **Tolérance au démarrage & Résilience temps réel** : un token enseignant expiré ne bloque jamais le bootstrap public ; une reconnexion élève (y compris fermeture d'onglet) restaure pseudo, état courant, vote déjà émis et résultats finaux via `localStorage` scopé par `sessionId`.
3. **Feedback explicite systématique** : zéro erreur silencieuse (upload image, soumission réponse élève, commandes présentateur, sauvegarde/lancement éditeur).

### 5.2 Découpage en Lots Parallélisables (Fichiers Strictement Disjoints)

| Lot | Périmètre & Responsabilité | Fichiers Propriétaires (Zéro Collision) | Dépendances |
| :--- | :--- | :--- | :--- |
| **LOT-F1 : Bootstrap, Auth & Core Intercepteurs** | Corriger `AppInitializer` non-bloquant, `AuthenticatedUserStore.refresh` (support `_temporaryTokens`), `refreshSessionInterceptor` (`IS_REFRESHING_ACCESS_TOKEN` + mutex `shareReplay`), `AuthenticationHttpService` (`sdk.gen.ts` + `{ identifier }`), `AccountActivationHttpService`, `RegisterHttpService`, `RegisterModal`, `ForgottenPassword`, `Login`, `ResetPassword`, `ResetPasswordForm`. | `front/src/app/app.initializer.ts`, `front/src/app/shared/stores/authenticated-user.ts`, `front/src/app/core/interceptors/refresh-session/**`, `front/src/app/features/auth/**`, `front/src/app/features/activation/**`, `front/src/app/features/admin/account/**` | Aucune |
| **LOT-F2 : Shell Global, PWA, Styles & Home** | Retirer `pFocusTrap` conflictuel de `app.html`, corriger `overflow-scroll` dans `styles/index.css`, configurer `ngsw-config.json` (`navigationUrls` + stratégie `freshness` thèmes), nettoyer `pages/home/home.*` (H1 + CTA si connecté), `type-selector.html` (`⯑` supprimé -> ou dans F4 selon frontière). *Note : laisser `type-selector.html` dans LOT-F4 pour garder zéro collision.* | `front/src/app/app.html`, `front/src/styles/index.css`, `front/ngsw-config.json`, `front/public/site.webmanifest` (suppression doublon si validé), `front/src/app/pages/home/**`, `front/nginx.conf` | Aucune |
| **LOT-F3 : Admin Dashboard, Thèmes & Pagination** | Corriger `pageSize` -> `perPage` dans `ThemeHttpService` (`sdk.gen.ts` + `catchError`), supprimer métriques factices et double `class` dans `admin/dashboard/home/*`, ajouter `takeUntilDestroyed` dans `ThemeCard` et `ThemeEditor`, corriger `SessionCreate` (`await firstValueFrom`, `quizzFieldError`, `SessionCreatedDto`) et `SessionEdit` / `SessionCard` / `SessionReport` (via `SessionHttpService.exportCsv` sans `HttpClient` brut, fix badges dark mode). | `front/src/app/features/admin/dashboard/**`, `front/src/app/features/admin/themes/**`, `front/src/app/features/admin/sessions/services/**`, `front/src/app/features/admin/sessions/stores/**`, `front/src/app/features/admin/sessions/components/**`, `front/src/app/features/admin/sessions/pages/{session-create,session-edit,session-list,session-report}/**` | Coordination DTO `ThemeName` / `PaginationDto` avec backend si modifié |
| **LOT-F4 : Éditeur QueZap & Upload Images WIP** | Finaliser `PictureUploadService` (erreurs HTTP typées, validation client JPEG/PNG/WEBP <= 5 Mo, `sdk.gen.ts`), corriger `QuestionEditor` & `AnswerComponent` (toasts d'erreur, `takeUntilDestroyed`, `ariaLabel`, masquer upload sur `Boolean`), réparer `TitleEditor` (`effect`), corriger `points ?? 1000`, `perPage` dans `QuezapHttpService`, nettoyer contrôles factices (`mediaFormat`/`backgroundTheme` soit persistés soit retirés, `"Pas de limite"` retiré si min=5s, sélecteur de Thème par question/quiz), optimiser sidebar gauche (`QuestionListView` + `role="listbox"`), et uniformiser URL de lancement. | `front/src/app/core/services/picture-upload/**`, `front/src/app/features/admin/quezaps/**` | Aucune (sauf si champs thème/explication ajoutés au contrat OpenAPI) |
| **LOT-F5 : Session Presenter Vidéoprojecteur** | Protéger ou gérer proprement l'auth + erreurs sur `SessionPresenter` (`features/routes.ts`), supprimer `HttpClient` brut (utiliser `SessionService.exportCsv`), ajouter contrôles `mobileCorrectionEnabled` & `explanationDurationSeconds`, synchroniser timer sur `payload.deadline`, corriger reload `STARTED`/`REVEAL`, corriger collision Cash+Image, réafficher l'image question au Reveal, remplacer double `[class]`, rendre bouton kick visible au tactile, ajuster Podium `720p` + lien vers Rapport. | `front/src/app/features/routes.ts`, `front/src/app/features/admin/sessions/pages/session-presenter/**` | `SessionService.exportCsv` du LOT-F3 |
| **LOT-F6 : Parcours Élève Live (`quizz/**`)** | Corriger `hasActiveSessionGuard` (`endedAt === null`), migrer `submittedChoice` & `finalResults` vers `localStorage` scopé par `sessionId`, gérer échecs `submitAnswer` (rollback optimiste + alerte 409/403/réseau), verrouiller votes pendant `isSessionPaused()`, optimiser mobile `390×844` (`sr-only` sur `"Image de la question"`, `max-height`), corriger `QuizzQuestionView` (>= 2 réponses), reconnexion `EventSource` robuste dans `SessionObserverSseService`, améliorer `/quizz/ended` (fallback `/scores` + message non-votant). | `front/src/app/features/quizz/**` | Aucune |

---

## 6. Critères d'Acceptation Vérifiables (Tests Unitaires Jest & E2E Playwright)

### 6.1 Parcours Auth & Boot
- **CA-AUTH-01** : Avec un `auth_tokens` contenant un `accessToken` expiré et un `refreshToken` valide en `localStorage`, le rechargement de `/` ou `/admin` rafraîchit silencieusement le token sans `window.confirm` ni déconnexion.
- **CA-AUTH-02** : Avec des tokens invalides en `localStorage`, l'ouverture de `/quizz/join` par un élève purge le token périmé silencieusement et affiche l'écran de connexion au quiz sans erreur bloquante.
- **CA-AUTH-03** : Sur `/auth/login`, des identifiants invalides (`401`) affichent le message `"Email ou mot de passe invalide"` sans toast technique générique.
- **CA-AUTH-04** : Dans `RegisterModal` et `ForgottenPassword`, la saisie d'un email valide active le bouton de soumission et envoie exactement `{ identifier }` sur `/apiv1/auth/forgotten-password`.

### 6.2 Upload Images & Éditeur QueZap
- **CA-EDIT-01** : L'upload d'une image PNG/JPEG/WEBP <= 5 Mo sur une question ou une réponse affiche un spinner, puis la prévisualisation avec URL `/quezap-public-images/...`, et est persisté via `POST/PUT /apiv1/quezaps`.
- **CA-EDIT-02** : L'upload d'un fichier non supporté (ex. `.gif`, `.txt`) ou > 5 Mo ou une erreur serveur `400/413/415/500` stoppe le spinner et affiche un toast PrimeNG `error` explicite (zéro échec silencieux).
- **CA-EDIT-03** : À l'ouverture de `/admin/quezaps/:id/edit`, `TitleEditor` affiche immédiatement le titre existant du quiz sans clic préalable.
- **CA-EDIT-04** : Une question configurée à `0 pt` conserve `0` après sauvegarde et réouverture de l'éditeur.
- **CA-EDIT-05** : Sur une résolution `1920×1080`, la barre latérale gauche avec 3 questions ne présente pas de barre de défilement inutile ni d'espace mort de 52px sur les cartes non sélectionnées.

### 6.3 Admin Listes, Sessions & Session Presenter
- **CA-ADM-01** : Sur `/admin/themes`, `/admin/quezaps` et `/admin/sessions`, les requêtes HTTP utilisent `perPage` (et non `pageSize`) ; naviguer en page 2 charge bien les éléments de la page 2.
- **CA-ADM-02** : Le Dashboard `/admin/dashboard` n'affiche aucune donnée synthétique/codée en dur (`count * 24` ou `78%`).
- **CA-PRES-01** : Sur `/admin/sessions/:id/present` (`1920×1080` et `1280×720`), l'enseignant peut activer/désactiver `mobileCorrectionEnabled`, ajuster `explanationDurationSeconds`, basculer `MANUAL`/`AUTOMATIC`, mettre en pause, exclure un élève (y compris sur écran tactile sans hover), voir l'image question en `QUESTION` et `REVEAL` (y compris sur une question `CASH` illustrée), et télécharger le CSV via `SessionService` (sans erreur ni fuite).
- **CA-PRES-02** : Sur `PODIUM` (`1280×720` et `1920×1080`), l'intégralité du Top 5, des métriques de classe, du bouton CSV et du lien vers le bilan tient dans `100vh` sans barre de scroll ni coupure.

### 6.4 Parcours Élève Mobile (`390×844`) & Temps Réel SSE
- **CA-PLAY-01** : Si l'élève ferme son onglet puis rouvre `/quizz/join` ou `/quizz` pendant la partie en cours, il réintègre la session sans ressaisir son pseudo, retrouve la question active, sa réponse déjà envoyée (`mySubmittedChoice` conservé dans `localStorage`), et voit les résultats finaux sur `/quizz/ended` à la fin du quiz.
- **CA-PLAY-02** : Si `POST /apiv1/sessions/{id}/answers` échoue (`409`, `400`, `403`, offline), le bandeau `"✓ Choix enregistré !"` n'apparaît pas (ou est annulé) et un message clair avertit l'élève.
- **CA-PLAY-03** : Lorsque `isPaused === true`, les choix de vote (QCM, Binary, Boolean, Cash) sont verrouillés (`inert`/`disabled`) jusqu'à la reprise.
- **CA-PLAY-04** : Sur un viewport `390×844`, une question QCM 4 choix avec image s'affiche sans texte visible parasite `"Image de la question"` et permet d'atteindre confortablement les 4 choix de vote.
- **CA-PLAY-05** : `hasActiveSessionGuard` redirige vers `/quizz/expired` si `session.endedAt !== null`.

### 6.5 Qualité & Accessibilité Globale
- **CA-QUAL-01** : `cd front && bun run lint && bun run test && bun run build` passe à **0 erreur**, **0 avertissement Stylelint/ESLint**, et **0 worker force-exit Jest**.
- **CA-QUAL-02** : Navigation clavier (`Tab`, `Enter`, `Escape`) fonctionnelle dans `QuezapEditor` (y compris ouverture/fermeture des `p-select` et `p-confirmdialog`), `QuizzRunner` (`ExitButton` popup) et `SessionPresenter` (`Image` preview et modal kick) sans piégeage par `pFocusTrap`.

---

## 7. Apprentissages / Règles à Ajouter à `AGENTS.md`

Les incidents identifiés durant cet audit montrent des pièges récurrents propres à la stack Angular 21 + Hey-API + PrimeNG v21 + PWA. Il est recommandé d'ajouter ces règles explicites dans `AGENTS.md` :

1. **Signal Forms + `quizzFieldError`** : Toute balise `<input>` utilisant `quizzFieldError` DOIT obligatoirement porter **à la fois** `[formField]="monForm.champ"` et `[fieldState]="monForm.champ"`. Ne jamais poser `quizzFieldError [fieldState]` seul (le champ semblerait fonctionner visuellement tout en restant désynchronisé du modèle).
2. **Interdiction des appels `client.get/post/put/patch/delete` manuels** : Utiliser exclusivement les fonctions générées de `sdk.gen.ts` (`login`, `forgottenPassword`, `find`, `create1`, `exportCsv`, `upload`, etc.) afin que le compilateur TypeScript garantisse les noms de champs (`identifier` vs `email`, `perPage` vs `pageSize`) et les DTOs exacts (`SessionCreatedDto` vs `SessionDetailDto`). En dernier recours, ne jamais typer `client.post<MonDto>` sans dictionnaire de statut HTTP `{ 200: MonDto }`.
3. **Interdiction totale d'injecter `HttpClient` dans les composants/pages** : Tout appel API (y compris téléchargement de `Blob` CSV) passe par un service métier dédié basé sur `sdk.gen.ts`.
4. **Piège `pFocusTrap` + PrimeNG `overlayAppendTo: 'body'`** : Ne jamais poser `pFocusTrap` sur le `<main>` applicatif global lorsque PrimeNG rend ses overlays (`p-select`, `p-confirmdialog`, `p-confirmpopup`, `p-image`) dans `<body>`. Réserver le piégeage de focus aux modales elles-mêmes.
5. **Valeurs numériques `0` valides (`??` vs `||`)** : Utiliser systématiquement `??` (et jamais `||`) pour les valeurs par défaut de `points`, `rank`, `score`, `timer` où `0` est une valeur métier légitime.
6. **Classes CSS Angular & HTML** : Ne jamais dupliquer l'attribut statique `class="..."` sur un élément HTML, et éviter de combiner `class="..."` avec un binding `[class]="expr"` (utiliser `[ngClass]` ou `class="... {{ expr }}"` / bindings booléens `[class.foo]="..."`).
7. **Persistance multi-onglets / fermeture d'onglet élève** : Ne jamais stocker l'état de reprise élève (`submittedChoice`, `finalResults`, `participantName`) uniquement dans `sessionStorage` ; utiliser `localStorage` scopé par `sessionId` avec nettoyage explicite à l'expiration/changement de session.
8. **Zéro métrique factice & zéro échec silencieux** : Ne jamais calculer de KPIs synthétiques côté frontend ni ignorer `isFailure(output)` sur les actions utilisateur (upload, vote, sauvegarde, pilotage live).
9. **Service Worker PWA (`ngsw-config.json`)** : Déclarer `navigationUrls` excluant `!/apiv1/**` et `!/quezap-public-images/**`, et ne jamais cacher en stratégie `performance` des endpoints de gestion CRUD (ex. `/apiv1/themes/**`).
10. **Upload d'images (`PictureUploadService`)** : Synchroniser `accept` et la validation client sur la liste blanche exacte du backend (`image/jpeg,image/png,image/webp`, `<= 5 Mo`), gérer chaque erreur dans `subscribe` avec `takeUntilDestroyed(destroyRef)` et un toast explicite.
