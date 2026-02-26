# Backlog MVP – Quiz interactif (état, complexité, priorité)

## Tâches déjà réalisées (MVP validé)
- [x] Génération et gestion d’un code session unique (pour rejoindre une partie)
- [x] Affichage des questions à tous les participants en temps réel
- [x] Timer visible pour chaque question
- [x] Système de réponses (choix unique/multiple, vrai/faux)
- [x] Feedback immédiat après chaque réponse (bonne/mauvaise, points gagnés)
- [x] Saisie du pseudo (obligatoire)
- [x] Répondre aux questions
- [x] Accès admin simplifié (flag/mock, pas de gestion fine des rôles)
- [x] Rafraîchissement de page = perte de session (comportement actuel en mock)

## 1. Calcul des scores et classement (PRIO 1)
- [~] Calcul automatique des scores (structure présente, scoring à fiabiliser et afficher) — Complexité : 3
- [~] Affichage du classement intermédiaire et final (structure présente, UI à compléter) — Complexité : 3

## 2. Contrôle animateur/admin (PRIO 2)
- [~] Interface pour lancer la session (bouton debug, à sécuriser et rendre admin-only) — Complexité : 2
- [~] Passer à la question suivante (logique mockable, UI à rendre accessible à l’admin) — Complexité : 2
- [~] Voir le classement en temps réel (structure présente, UI à compléter) — Complexité : 3

## 3. Lobby d’attente (PRIO 3)
- [~] Lobby d’attente (liste participants, statut prêt/en attente) — UI et mock présents, à finaliser pour UX réelle — Complexité : 2

## 4. Sécurité & robustesse (PRIO 4)
- [~] Empêcher de répondre plusieurs fois à la même question (partiellement géré, à fiabiliser) — Complexité : 2

## 5. Responsive/adaptatif (PRIO 5)
- [~] Interface responsive/mobile (bonne base, à valider sur tous les écrans) — Complexité : 2
- [~] Responsive sur tous les écrans clés (bonne base, à valider/ajuster) — Complexité : 2

## 6. Correction des bugs bloquants (PRIO 6)
- [~] Correction des bugs bloquants (à tester en conditions réelles) — Complexité : 2

## 7. (Optionnel) Avatar participant (PRIO 7)
- [ ] (Optionnel) Choix d’un avatar simple (emoji ou image par défaut) — Complexité : 1

---

// Les autres fonctionnalités avancées sont à reporter après MVP.
