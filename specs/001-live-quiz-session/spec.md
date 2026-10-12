# Feature Specification: Diffusion et animation de quiz interactif en classe

**Feature Branch**: `001-live-quiz-session`

**Created**: 2026-10-12 (Mis à jour suite à l'analyse de l'existant)

**Status**: Draft

**Input**: User description: "Diffusion de questions de la part de professeur au travers de sessions, et permettre a des eleves de rejoindre la session et de repondre au questions. Le professeur passe au question suivante manuellement, ou alors il peut choisir de passer automatiquement ainf que le quizz se deroule jusqu'au bout. Les eleves repondent a chaque questions, il voient ensuite uniquement la question et la reponse qu'ils ont choisi pour les inciter a regarder le tableau et suivre les instructions du professeurs. Avec d'eventuelles explications. A cet effet il faudra que le professeur puisse mettre en pause la question empechant de passer automatiquement a la question suivante afin qu´il ai le temps de donner des explications. On affichera le score qu'a la fin (sans scroll si possible), donc uniquement ceux en tetes (5 premiers par exemple), au tableau. et chaque eleve vois le classement entier sur sont ecran à la fin du quizz. Le but est à visée pédagogique et non mercantile, il faut pouvoir captiver les élèves et les inciter a êtres concentrés et studieux, ceci dans un esprit de compétition sain."

## Clarifications

### Session 2026-10-12
- Q: En mode automatique, quelle doit être la durée d'affichage de la correction avant de passer à la question suivante ? → A: 15 secondes par défaut (adapté aux élèves en difficulté), avec durée configurable/réglable par l'enseignant.
- Q: Comment et quand la validité/correction de la réponse doit-elle s'afficher sur le téléphone de l'élève ? → A: Révélation différée synchronisée avec le tableau. L'élève voit toujours l'énoncé et sa propre réponse choisie. L'affichage de la correction (bonne réponse / statut correct ou incorrect) sur le téléphone est désactivé par défaut, mais peut être activé optionnellement par l'enseignant.
- Q: Comment la mise en pause explications doit-elle être signalée visuellement ? → A: Affichage bien visible d'un texte "En pause" pour tout le monde sur tous les écrans (tableau enseignant et smartphones des élèves), avec gel/freeze immédiat du timer.
- Q: Comment concilier émulation saine, inclusion (élèves RQTH/en difficulté) et esprit de groupe sur les écrans de fin ? → A: Présentation épurée sans fioritures superflues. Au tableau : affichage du Top 5 compact sans scroll, complété par les métriques collectives de groupe (moyenne de score de la classe, pourcentage collectif de bonnes réponses) pour que la classe se situe en tant que groupe soudé. Sur mobile : chaque élève se situe sans sentiment d'exclusion (restitution bienveillante de sa progression individuelle + classement général complet accessible avec repère de la moyenne de classe).
- Q: Quelles sont les contraintes de rétrocompatibilité avec les sessions ou schémas existants ? → A: Aucune rétrocompatibilité requise (développement au niveau zéro / greenfield, reset complet de l'état/schémas accepté sans compromis architectural).

## État de l'existant & Écarts identifiés (Gap Analysis)

L'étude approfondie du codebase existant révèle les acquis et les ajustements requis :

1. **Ce qui existe déjà (fondations solides)** :
   - **Backend** : `Session` (agrégat DDD), persistance `SessionJpaEntity`, flux SSE complet dans `SessionSseService` (`SESSION_STARTED`, `QUESTION_START`, `QUESTION_REVEAL`, `SESSION_ENDED`, etc.), calcul de scores dégressifs avec chrono, soumission des réponses (`POST /sessions/{id}/answers`).
   - **Écran Enseignant (`SessionPresenter`)** : Gestion du salon d'attente (QR code, participants), affichage question grand écran, révélation avec statistiques et explications, écran de podium.
   - **Écran Élève (`QuizzRunner` & `Ended`)** : Écoute SSE via `ActiveSessionStore` et `SessionObserverSseService`, saisie de pseudo, soumission de réponse, écran de fin `Ended`.

2. **Écarts et modifications clés par rapport à votre besoin** :
   - **Défilement automatique et Pause explications** :
     - *Existant* : Actuellement, le passage est 100% manuel via des clics sur `revealAnswers()` et `nextStep()`.
     - *À ajouter* : Un sélecteur de mode ("Manuel" vs "Automatique") côté enseignant. En mode automatique, un timer enchaîne la question, la révélation de la correction, puis la question suivante. Un bouton **"Pause explications"** doit permettre à l'enseignant de bloquer la transition automatique pour garder la question et ses explications au tableau le temps nécessaire.
   - **Écran Élève après vote (Sobriété anti-distraction)** :
     - *Existant* : Lors de la révélation (`QUESTION_REVEAL`), l'élève voit son rang intermédiaire `#rank`, ses points gagnés et un bouton intermédiaire de classement si diffusé.
     - *À ajuster* : Dès la réponse validée (et durant toute la phase de vote / révélation), masquer tout classement ou score intermédiaire sur le smartphone de l'élève. L'écran mobile doit afficher sobrement l'intitulé et la réponse sélectionnée par l'élève, complété d'une incitation claire à regarder le tableau ("Regarde le tableau pour les explications").
   - **Classement au tableau vs Classement mobile à la fin du quiz** :
     - *Existant tableau* : Le tableau (`stage === 'PODIUM'`) affiche les 3 marches du podium puis déroule sous forme de liste scrollable la totalité des élèves (`leaderboard()`), ce qui force le défilement vertical sur grand groupe.
     - *À ajuster tableau* : Restreindre l'affichage au tableau au **Top 5 uniquement**, conçu sans scroll pour tenir parfaitement sur l'écran du vidéoprojecteur.
     - *Existant élève* : La page finale `Ended` affiche uniquement les métriques personnelles de l'élève (rang, score, bonnes réponses) mais **ne liste pas** le classement général.
     - *À ajuster élève* : Stocker et afficher le tableau ordonné complet de toute la classe dans la page de fin de l'élève (`Ended`), lui permettant de faire défiler tout le classement à son rythme sur son propre appareil.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Contrôle de la diffusion par l'enseignant : Manuel vs Automatique & Pause explications (Priority: P1)

En tant qu'enseignant, je souhaite pouvoir lancer un quiz soit en mode manuel (transition au clic), soit en mode automatique (enchaînement chronométré continu des questions), avec la faculté d'activer une pause explications à tout instant en mode automatique afin d'interrompre le défilement et commenter la réponse au tableau sans précipitation.

**Why this priority**: C'est le cœur de la gestion du temps pédagogique en classe, permettant à la fois un déroulement fluide et des temps d'arrêts explicatifs essentiels.

**Independent Test**: L'enseignant active le mode automatique. À la fin du chrono de réponse, la correction s'affiche. L'enseignant clique sur "Pause explications" : le décompte de transition se fige. Il commente son cours, désactive la pause, et le quiz passe automatiquement à la question suivante.

**Acceptance Scenarios**:

1. **Given** une session en mode manuel, **When** le temps d'une question expire ou que l'enseignant clique sur "Révéler", **Then** les réponses correctes et l'explication sont projetées au tableau, et aucune transition n'a lieu tant que l'enseignant ne clique pas sur "Question suivante".
2. **Given** une session en mode automatique, **When** le temps imparti à une question expire, **Then** la révélation s'affiche automatiquement avec un compte à rebours de lecture (ex. 10s), suivi du passage automatique à la question suivante.
3. **Given** une session en mode automatique avec compte à rebours de transition actif, **When** l'enseignant active la "Pause explications", **Then** le décompte s'interrompt immédiatement, la projection reste figée sur l'explication, et la transition ne reprend pas tant que l'enseignant ne reprend pas la lecture ou ne force pas manuellement la question suivante.

---

### User Story 2 - Écran élève recentré sur l'écoute et le tableau (Priority: P1)

En tant qu'élève, dès que j'ai soumis mon vote (ou que le temps de vote est écoulé), je souhaite voir sur mon smartphone un rappel épuré de la question et du choix que j'ai fait, sans animations de score ou de rang intermédiaire distrayantes, afin de lever les yeux vers le tableau et écouter les explications de l'enseignant.

**Why this priority**: Évite que les élèves ne gardent les yeux rivés sur leur écran de téléphone ou ne soient distraits/démotivés par un classement en cours de partie.

**Independent Test**: Un élève sélectionne une réponse. Son écran se fige sur un affichage propre rappelant sa proposition et l'énoncé avec la mention "Regarde le tableau pour les explications", sans affichage de podium ou de score intermédiaire.

**Acceptance Scenarios**:

1. **Given** une question en cours de vote, **When** l'élève clique sur une proposition, **Then** son choix est verrouillé et son écran affiche sobrement l'énoncé et la réponse qu'il a sélectionnée.
2. **Given** la phase de révélation déclenchée par l'enseignant, **When** l'élève regarde son smartphone, **Then** son écran n'affiche aucun classement intermédiaire ni spoiler supplémentaire, invitant explicitement à regarder l'explication au tableau.
3. **Given** un élève n'ayant pas voté dans le temps imparti, **When** le chrono expire, **Then** son écran indique sobrement "Temps écoulé - Regarde le tableau".

---

### User Story 3 - Épilogue du quiz : Top 5 sans scroll au tableau et classement complet privé sur mobile (Priority: P2)

En tant qu'enseignant et élèves, nous découvrons les résultats uniquement à la toute fin du quiz : le tableau projette un Top 5 valorisant et lisible sans ascenseur vertical (scroll), tandis que chaque élève peut consulter sur son mobile son score, son rang et l'intégralité du classement général de la classe.

**Why this priority**: Préserve la motivation jusqu'à la fin du jeu, évite la stigmatisation publique des derniers élèves sur le vidéoprojecteur, tout en offrant la transparence complète à chaque élève sur son téléphone.

**Independent Test**: Dès la clôture du quiz, le vidéoprojecteur affiche le Top 5 dans un gabarit fixe de 5 cartes/marches sans défilement vertical. Sur son smartphone, l'élève retrouve sa carte de résultats et la liste déroulante complète de tous les élèves classés.

**Acceptance Scenarios**:

1. **Given** la fin du quiz déclenchée, **When** le tableau affiche le podium final, **Then** seuls les 5 premiers élèves sont affichés au grand écran, dimensionnés pour s'intégrer à 100% dans la hauteur de la fenêtre sans défilement (scroll) vertical.
2. **Given** l'écran de fin affiché, **When** un élève consulte sa page de fin (`/quizz/ended`), **Then** il voit son rang personnel, ses points, son nombre de bonnes réponses, et peut consulter la liste ordonnée complète de l'ensemble des participants.
3. **Given** une classe de moins de 5 élèves, **When** le classement s'affiche, **Then** seuls les participants réels sont affichés sans case vide incohérente.

---

### Edge Cases

- **Mise en pause pendant le compte à rebours de vote** : Si l'enseignant suspend le quiz pendant la question elle-même, le chronomètre de réponse s'arrête également pour permettre un éclaircissement avant de reprendre.
- **Bascule de mode en plein vol** : L'enseignant peut basculer du mode Automatique au mode Manuel (et inversement) à n'importe quel moment de la session sans corrompre l'état de la question courante.
- **Égalité de points dans le Top 5** : Les ex æquo sont départagés par le temps cumulé de réponse le plus court, garantissant toujours au maximum 5 lignes au tableau sans débordement vertical.
- **Reconnexion d'un élève après soumission** : L'élève qui rafraîchit sa page ou subit une micro-coupure retrouve directement l'écran sobre rappelant sa réponse déjà transmise.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Le système DOIT permettre à l'enseignant de choisir le mode de déroulement de la session : **Manuel** ou **Automatique**.
- **FR-002**: En mode Automatique, le système DOIT enchaîner automatiquement :
  - La fin du chrono de vote
  - L'affichage de la révélation et de l'explication (durée réglable par l'enseignant, avec 15 secondes par défaut pour s'adapter aux élèves en difficulté de lecture/compréhension)
  - Le passage à la question suivante jusqu'au terme du quiz.
- **FR-003**: Le système DOIT fournir à l'enseignant un contrôle de **Pause explications** accessible en permanence pour suspendre immédiatement toute transition automatique programmée ou figer le chrono en cours.
- **FR-004**: Dès l'activation de la pause, le système DOIT figer immédiatement le compte à rebours et afficher un texte/bandeau **"En pause"** bien visible pour tout le monde sur l'ensemble des écrans (grand écran enseignant et téléphones des élèves) via l'événement SSE de synchronisation.
- **FR-005**: Dès qu'un élève a validé son vote, son interface DOIT remplacer les boutons de sélection par un écran sobre affichant uniquement l'énoncé de la question, la réponse qu'il a choisie, et l'invitation à regarder le tableau.
- **FR-005b**: Par défaut, aucune indication de succès/échec ou de correction détaillée n'est affichée sur le téléphone de l'élève (la correction se suit au tableau). Le système DOIT permettre à l'enseignant d'activer, s'il le souhaite, l'affichage de la correction sur le téléphone des élèves lors de la phase de révélation.
- **FR-006**: Le système NE DOIT PAS afficher de classement intermédiaire aux élèves sur leurs téléphones pendant le déroulement du quiz.
- **FR-007**: À la fin du quiz, l'écran public enseignant (vidéoprojecteur) DOIT afficher le classement restreint aux **5 premiers participants maximum** (Top 5) dans un conteneur sans fioritures et sans défilement vertical (*no-scroll layout*), complété par les métriques collectives de réussite de la classe (moyenne des scores, pourcentage global de bonnes réponses).
- **FR-008**: À la fin du quiz, l'événement SSE `SESSION_ENDED` DOIT transmettre le classement intégral de tous les participants (`finalLeaderboard`) ainsi que les métriques collectives calculées (moyenne de la classe, total de participants).
- **FR-009**: La page de fin élève (`quizz/ended`) DOIT afficher une restitution individuelle bienveillante (score, rang, bonnes réponses, comparaison positive par rapport à la moyenne du groupe) et permettre de consulter le classement complet de la classe avec la ligne repère de la moyenne.
- **FR-010**: L'écran enseignant DOIT continuer de proposer l'export des résultats en CSV pour le bilan pédagogique.

### Key Entities & Data Extensions

- **Session (Agrégat / Entité)** : Ajout du mode de déroulement (`ProgressionMode: MANUAL | AUTOMATIC`) et de l'état de pause (`isPaused: boolean`).
- **QuestionReveal & Explication** : Déjà pris en charge par `currentQuestion.explanation` et diffusé via `QUESTION_REVEAL`.
- **FinalLeaderboardEntry** : Structure contenant le rang, le nom, le score total et le nombre de bonnes réponses, conservée pour le classement complet côté élève.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En mode automatique, la pause explications bloque la transition en moins de 100 ms côté enseignant sans saut inopiné vers la question suivante.
- **SC-002**: L'écran de fin du vidéoprojecteur (Top 5) s'affiche intégralement dans le viewport sans aucune barre de défilement verticale sur un écran en 1080p et 720p.
- **SC-003**: 100% des élèves ayant validé leur réponse ont une interface statique sans distraction visuelle jusqu'à la question suivante.
- **SC-004**: 100% des élèves peuvent consulter l'intégralité du classement sur leur propre appareil une fois le quiz terminé.
- **SC-005**: Le nouveau modèle et les flux de session sont conçus de manière optimale et cohérente sans compromis de rétrocompatibilité (démarche greenfield / niveau zéro).

---

## Assumptions

- L'enseignant diffuse l'écran `session-presenter` sur un vidéoprojecteur ou un écran partagé en classe.
- Le rythme par défaut de temporisation de l'explication en mode automatique est fixé à 15 secondes et reste modifiable par l'enseignant dans les paramètres de la session (sauf pause explications).
- Les mécanismes SSE existants (`SessionSseService` et `SessionObserverSseService`) sont conservés et complétés pour relayer l'état de pause si nécessaire.
