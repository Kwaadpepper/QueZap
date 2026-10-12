# Original User Request

## 2026-10-12T06:36:52Z

Audit (lecture seule, AUCUNE implémentation) de l'application QueZap (quiz temps réel : écran enseignant + mobiles élèves via SSE) par une brigade de 4 experts spécialisés qui délibèrent via un Blackboard partagé et produisent un plan d'action pour rendre l'application complètement utilisable et commercialement irrésistible, sans la dénaturer.

Working directory: /home/jeremy/projets/QueZap
Integrity mode: development

## Contexte déjà fourni (étape 1 faite)
- Stack déjà lancée : frontend `http://localhost:8000`, Postgres/MinIO/backend via podman. Login admin : `admin@quezap.com` / `Password123!`.
- 15 captures déjà dans `.audit/screenshots/` (index : `.audit/screenshots/INDEX.txt`) : landing, login, reset, about, admin (dashboard, sessions, sessions/create, quezaps, quezaps/create, themes), élève mobile (join, join/ABC123, expired, ended). Script réutilisable : `.audit/capture.cjs`.
- **Manquent** (états live) : lobby hôte (PIN/QR/joueurs), question en cours, reveal, classement, podium, rapport de session ; côté élève : lobby, vote actif, feedback, fin de partie. Les experts devront les capturer (simulation 1 hôte + N élèves, cf. skill `.agents/skills/e2e-playwright/SKILL.md`).
- Constat déjà visible : icône manquante sur la carte « Créer un Quiz » du dashboard.
- L'application doit respecter `AGENTS.md` (DDD hexagonal, NullAway, zéro `any`, Signals, Hey-API/Zod, GraalVM).

## Requirements

### R1. Complétion de la couverture visuelle
Capturer les écrans live manquants (hôte 1920x1080, élèves 390x844) dans `.audit/screenshots/`, nommés explicitement (`host_20_lobby.png`, `player_03_active_vote.png`…).

### R2. Délibération en Blackboard
Quatre experts — (1) CPO & stratégie commerciale (vs Kahoot/Quizizz/Wooclap, onboarding 30 s, élève sans compte), (2) UI/UX & design system (lisibilité à 10 m, thumb zone, cohérence Tailwind/PrimeNG), (3) Lead engineer (conformité AGENTS.md, robustesse SSE/reconnexion/timer, faisabilité), (4) QA & accessibilité (daltonisme/formes, cas limites : pseudo long, déconnexion, 35 élèves) — collaborent via `.audit/BLACKBOARD.md` : Facts & Visual Evidence, Specialist Contributions, Cross-Examination & Debates, Consensus & Master Action Plan. Trois tours : diagnostic, confrontation, synthèse.

### R3. Plan d'action de victoire
Produire `.audit/ACTION_PLAN.md` priorisé P0 (quick wins/bloquants), P1 (évolutions produit), P2 (robustesse technique/a11y/edge cases). Chaque tâche : rôle assigné, fichiers cibles Front/Back réels, critères d'acceptation stricts. Le produit ne doit pas être dénaturé.

### R4. Aucune modification du code
Seuls `.audit/` peut être écrit. Le code source n'est pas modifié.

## Acceptance Criteria

### Couverture
- [ ] Chaque écran listé en R1 est capturé, ou son absence est justifiée avec preuve (erreur, log) dans le Blackboard

### Délibération
- [ ] BLACKBOARD.md contient les 4 sections, ≥1 contribution par expert et ≥1 réponse croisée par expert aux autres
- [ ] Chaque constat important cite une capture ou un fichier/ligne

### Plan
- [ ] Chaque tâche cite des fichiers existants (vérifiés) et un critère d'acceptation vérifiable par commande ou capture
- [ ] Le plan couvre les 4 axes (commercial, UX, technique, a11y/QA) et chaque expert a validé le consensus
- [ ] `git status` ne montre aucune modification hors `.audit/`


## Follow-up — 2026-10-12T06:51:50Z

tu va mettre en pause quand le dernier subagent termine je dois mettre en veille le laptop
