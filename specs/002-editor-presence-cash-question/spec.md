# Feature Specification: Editor Layout, Player Reconnection & Presence, Large Media, Cash Question Mode

**Feature Branch**: `002-editor-presence-cash-question`

**Created**: 2026-10-17

**Status**: Draft

**Input**: User description: "sur l'editeur la colonne de gauche on a un scroll au lieux d'afficher le contenu entier de la barre. si on eleve ferme l'onglet il et toujours dans la salle et si il rejoins de nouveau potentiellement il dois saisir un nouveau pseudo ce qui fait une personne en plus. Il faut que l'on identifie le client de facon unique pour eviter ce probleme (lui permettre de revenir en cas de coupure reseau), il faut aussi que si un eleve se deconnecte de la session cela soit pris en compte. - Avoir une image en plus grand sur le Quezap sur chaque question - avoir un mode réponse cash (l'élève écrit la réponse) - tulipe -> comparaison de texte en faisant slugify et en supprimante les accents (transliteration), emoji et double ou triple lettres."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reconnection & Détection de Déconnexion d'un Élève (Priority: P1)

En tant qu'élève participant à un quiz en direct, si je perds accidentellement ma connexion ou si je ferme par erreur mon onglet de navigateur, je dois pouvoir revenir instantanément dans la session en cours avec le même pseudo, sans être comptabilisé en double. Si je quitte volontairement la session ou si ma session expire, mon départ doit être reflété sur l'écran de l'enseignant et des autres participants.

**Why this priority**: Évite la prolifération de participants fantômes dans les sessions, garantit la robustesse réseau en environnement de classe (réseau Wi-Fi instable, fermeture involontaire d'onglets) et assure l'exactitude des scores et présences.

**Independent Test**:
- Rejoindre une session avec un pseudo et un identifiant unique stocké côté client (par exemple dans le stockage local du navigateur).
- Fermer l'onglet, rouvrir l'URL ou saisir le code de session : l'élève réintègre automatiquement la session sans ressaisie de pseudo et sans créer un second participant dans la liste.
- Fermeture définitive / déconnexion explicite : la présence du joueur est mise à jour en temps réel sur l'écran présentateur.

**Acceptance Scenarios**:
1. **Given** un élève connecté avec le pseudo "Léa", **When** il actualise la page ou ferme puis rouvre l'onglet du quiz, **Then** il réintègre sa session avec son état courant et ses points, sans créer de doublon "Léa 2".
2. **Given** un élève présent dans le lobby ou dans le quiz, **When** il clique sur "Quitter" ou perd sa connexion prolongée, **Then** la liste des participants sur le tableau présentateur et les écrans élèves met à jour le compteur et la présence sans nécessiter de rafraîchissement manuel.

---

### User Story 2 - Mode de Question "Cash" avec Tolérance Orthographique (Priority: P2)

En tant qu'enseignant créateur de quiz, je souhaite créer un nouveau type de question intitulé "Cash" (saisie de texte libre par l'élève) avec une ou plusieurs réponses attendues, afin d'évaluer la mémorisation directe sans proposition à choix multiples.
En tant qu'élève, je saisis ma réponse textuelle sur mon appareil ; le système valide ma réponse même en cas de variations mineures de typographie, d'accents, de casse, d'emojis ou de lettres répétées par inadvertance (ex: "tulipe" correspond à "Tulipe", "tuliipe", "tulipe 🌷").

**Why this priority**: Enrichit la pédagogie en offrant une modalité d'évaluation directe très demandée (format inspiré des quiz télévisés et concours scolaires), avec un moteur de comparaison tolérant mais rigoureux.

**Independent Test**:
- Créer une question de type "Cash" dans l'éditeur avec la réponse correcte "tulipe".
- Lancer la session : l'élève voit un champ de saisie texte avec bouton de validation au lieu de tuiles de couleurs.
- Soumettre des variantes textuelles ("Tulipe", "tuliipe", "tùlipe", "tulipe 🌸", "tulllipe") : vérifier qu'elles sont reconnues comme correctes.
- Soumettre une réponse erronée ("rose") : vérifier qu'elle est comptée fausse.

**Acceptance Scenarios**:
1. **Given** une question Cash avec la réponse configurée "tulipe", **When** l'élève répond "Tulipe !", "tuliipe" ou "túlipé", **Then** le moteur normalise la réponse (slugify, translittération sans accents, suppression ponctuation/emojis, dédoublonnage des répétitions consonantiques/vocaliques superflues) et valide la réponse avec les points accordés.
2. **Given** une question Cash en direct, **When** le temps imparti expire ou l'élève valide, **Then** la réponse est enregistrée et la révélation affiche la bonne réponse textuelle sur l'écran projecteur.

---

### User Story 3 - Affichage d'Image Agrandie pour les Questions (Priority: P3)

En tant qu'enseignant et élève, lorsque la question comporte une illustration (photo, schéma, carte), celle-ci doit pouvoir s'afficher en grand format lisible sur le vidéoprojecteur et être consultable confortablement sur mobile, sans être rétrécie ou étouffée par le décor du thème.

**Why this priority**: Indispensable pour les questions visuelles (cartes de géographie, graphiques, figures scientifiques, œuvres d'art) où les détails doivent être lisibles à distance par toute la classe.

**Independent Test**:
- Ajouter une image haute résolution sur une question dans l'éditeur.
- Lancer le quiz sur l'écran présentateur : l'image dispose d'un espace central agrandi, avec possibilité de l'agrandir en plein écran / zoom modal si nécessaire.
- Côté mobile élève : l'image est visible avec une taille adaptée et un ratio préservé.

**Acceptance Scenarios**:
1. **Given** une question avec image, **When** elle est projetée sur l'écran enseignant, **Then** l'image s'affiche avec une dimension généreuse (hauteur et largeur prépondérantes au-dessus des options de réponse), sans déformation d'aspect ratio.
2. **Given** une question avec image sur mobile élève, **When** l'élève observe la question, **Then** l'image est clairement visible et n'est pas masquée ou réduite à une vignette illisible.

---

### User Story 4 - Ergonomie de la Barre Latérale Gauche dans l'Éditeur (Priority: P4)

En tant qu'auteur de quiz dans l'éditeur, la barre latérale gauche listant les questions doit être fluide et optimisée : elle doit s'adapter à la hauteur d'écran disponible sans barre de défilement parasite lorsque le nombre de questions est restreint, et offrir une vue d'ensemble claire avec accès direct aux boutons d'ajout et d'organisation des diapositives.

**Why this priority**: Améliore le confort de création et l'aspect professionnel de l'interface d'édition pour les enseignants.

**Independent Test**:
- Ouvrir l'éditeur de QueZap avec 2 à 4 questions : vérifier que la colonne de gauche s'affiche sans ascenseur vertical tronqué et que le contenu et le bouton "Ajouter une question" sont immédiatement visibles.
- Ajouter un grand nombre de questions : vérifier que le défilement ne s'active qu'au-delà de la hauteur disponible de façon naturelle.

**Acceptance Scenarios**:
1. **Given** un quiz de 3 questions dans l'éditeur sur une résolution standard (1080p ou 768p), **When** l'utilisateur consulte la colonne de gauche, **Then** l'intégralité des cartes de questions et le bouton d'ajout s'affichent sans scrollbar forcée inutile.

---

### Edge Cases

- **Déconnexion brève (micro-coupure Wi-Fi)** : Si la connexion SSE est interrompue moins de 30 secondes, le client doit se reconnecter silencieusement avec son jeton et reprendre son état exact (question courante, réponse déjà soumise, minuterie restante).
- **Changement d'appareil avec même pseudo** : Si un élève tente de se connecter sur un autre appareil avec le même pseudo alors qu'un token actif est déjà associé à ce pseudo dans la session, le système doit refuser l'usurpation ou proposer de récupérer la session via un code de récupération court.
- **Normalisation de texte pour mots avec lettres doubles légitimes** : En français ou anglais, certains mots comportent des lettres doubles légitimes (ex: "carotte", "pomme", "balloon"). La règle de compression des lettres (tuliipe -> tulipe) ne doit pas casser les orthographes exactes répertoriées ou doit s'appliquer de façon symétrique sur la cible et sur l'entrée (ex: normaliser à la fois la réponse attendue et la réponse saisie par la même fonction canonique).
- **Plusieurs réponses acceptées en mode Cash** : L'enseignant peut définir des variantes acceptées (synonymes, par exemple "vélo", "bicyclette"). Si la réponse correspond à l'une quelconque des variantes normalisées, elle est comptée bonne.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Le système DOIT attribuer et conserver un identifiant persistant de participation (stocké côté client) pour chaque élève rejoignant une session, permettant une réintégration transparente en cas de rafraîchissement de page ou reconnexion réseau.
- **FR-002**: Le système DOIT détecter la fermeture explicite de la session ou la perte prolongée de connexion SSE d'un participant et notifier en temps réel l'écran présentateur et les autres participants pour mettre à jour la présence.
- **FR-003**: Le système DOIT supporter un nouveau type de question `CASH` dans l'éditeur, dans le domaine métier et sur les interfaces joueur / présentateur.
- **FR-004**: Pour les questions de type `CASH`, le système DOIT permettre à l'auteur de spécifier une ou plusieurs réponses textuelles acceptées.
- **FR-005**: Le système DOIT intégrer un algorithme de normalisation et de comparaison textuelle canonique comprenant :
  - Passage en minuscules et suppression des espaces superflus (trim).
  - Translittération complète (suppression des accents, cédilles, trémas via décomposition Unicode NFD).
  - Suppression de la ponctuation, des caractères spéciaux et des emojis.
  - Réduction des répétitions de lettres consécutives (2 ou plus ramenées à la forme canonique) appliquée identiquement sur la saisie et sur la réponse attendue.
- **FR-006**: Sur l'interface élève, lorsqu'une question de type `CASH` est active, le système DOIT présenter un champ de saisie textuel ergonomique avec clavier adapté et bouton de validation.
- **FR-007**: Sur les écrans présentateur et joueur, le composant d'affichage d'image DOIT proposer un affichage élargi/agrandi avec un ratio respecté, maximisant la lisibilité des illustrations de questions.
- **FR-008**: Dans l'éditeur de QueZap, le conteneur de la colonne latérale gauche des questions DOIT dimensionner dynamiquement sa hauteur (`flex`, `h-full`, `overflow-y-auto` conditionnel) pour éviter tout ascenseur vertical prématuré lorsque le contenu tient dans l'écran disponible.

### Key Entities *(include if feature involves data)*

- **ParticipantSession**: Représente la présence et l'identité d'un joueur dans une session active (identifiant unique persistant, pseudo, jeton de reconnexion, état en ligne / hors ligne, score cumulé).
- **CashQuestion**: Question sans choix multiples requérant une réponse textuelle directe avec une liste de réponses canoniques acceptées (`acceptedAnswers`).
- **TextNormalizer**: Service pur de canonisation de chaînes de caractères pour la comparaison tolérante de réponses textuelles.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% des rafraîchissements de page ou réouvertures d'onglets d'un élève pendant une session active restaurent la session existante sans duplication de joueur.
- **SC-002**: La comparaison en mode Cash valide avec succès les variations usuelles (accents, casse, emojis d'ornement, frappes doublées accidentelles) avec un taux d'exactitude de 100% sur les cas de test canoniques ("Tulipe", "tuliipe", "tulipe 🌷").
- **SC-003**: Les images de questions occupent au moins 40% de la surface centrale utile sur l'écran projecteur sans déborder ni altérer les contrôles de jeu.
- **SC-004**: La colonne de gauche de l'éditeur ne présente aucune barre de défilement tant que le nombre de diapositives n'excède pas la hauteur visible de la fenêtre.

## Assumptions

- Le stockage local du navigateur (`localStorage` / `sessionStorage`) est disponible et activé sur les appareils des élèves.
- Les questions de type Cash acceptent une chaîne de réponse courte (mots ou expressions courtes, généralement 1 à 4 mots) plutôt que des paragraphes entiers.
- Les règles architecturales du projet (DDD pur dans `modules:domain`, Zero `any`, NullAway, JSpecify, Signal Stores Angular 21, AOT GraalVM 25) s'appliquent à tous les nouveaux composants et Use Cases.
