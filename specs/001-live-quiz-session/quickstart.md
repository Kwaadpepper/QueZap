# Quickstart: Diffusion et animation de quiz interactif en classe

**Feature**: `001-live-quiz-session`  
**Date**: 2026-10-12

Ce guide permet de tester et valider le cycle complet de diffusion d'un quiz en classe avec un écran enseignant (vidéoprojecteur) et deux onglets élèves mobiles.

---

## 1. Démarrage de l'environnement local

```bash
# 1. Démarrer les services Docker (Postgres & MinIO)
docker compose up -d postgres minio minio-init

# 2. Démarrer le backend Spring Boot
./back/gradlew -p back :quezap-api:bootRun

# 3. Démarrer le frontend Angular
cd front && bun run start
```

---

## 2. Déroulement du scénario de test

### Étape 1 : Création et ouverture de session enseignant
1. Rendez-vous sur `http://localhost:4200/admin/sessions` (ou connectez-vous avec l'utilisateur enseignant seedé).
2. Lancez une session de quiz. L'écran grand format du présentateur s'ouvre sur `http://localhost:4200/admin/sessions/:id/presenter`.
3. Notez le code PIN court à 6 caractères ou le QR code affiché dans le salon d'attente.
4. Dans la barre de configuration du présentateur, activez le **Mode Automatique** (la durée par défaut d'explication est 15s).

### Étape 2 : Connexion des élèves (2 onglets séparés)
1. Ouvrez une fenêtre de navigation privée ou un autre navigateur sur `http://localhost:4200/quizz/join`.
2. Saisissez le code PIN et le pseudo "Élève 1", puis rejoignez le salon.
3. Répétez l'opération dans un second onglet avec "Élève 2".
4. Vérifiez que le tableau enseignant affiche immédiatement 2 joueurs connectés.

### Étape 3 : Lancement du quiz & Vote élève
1. Cliquez sur "Démarrer le Quizz" sur l'écran enseignant.
2. La question 1 apparaît au tableau avec son chrono de 30s.
3. Côté "Élève 1", sélectionnez une proposition :
   - Vérifiez que dès la sélection, les boutons sont figés et que l'écran affiche uniquement l'intitulé, votre choix, et l'invitation "👀 Regarde le tableau pour les explications".
   - Vérifiez qu'aucun classement intermédiaire n'est affiché.

### Étape 4 : Test de la Pause explications
1. En mode automatique, lorsque le temps de question s'achève, la révélation de la bonne réponse s'affiche avec son compte à rebours d'explication de 15s.
2. Sur l'écran enseignant, cliquez sur **"Pause explications"** :
   - Vérifiez que le compte à rebours de 15s se gèle immédiatement.
   - Vérifiez qu'un bandeau **"En pause"** très visible apparaît au tableau et sur les smartphones des élèves.
   - Vérifiez que la question suivante ne se déclenche pas tant que la pause est maintenue.
3. Cliquez à nouveau pour reprendre : le compte à rebours reprend et bascule automatiquement sur la question suivante.

### Étape 5 : Restitution finale (Émulation saine & inclusion)
1. À la fin de la dernière question :
   - **Au tableau (vidéoprojecteur)** : Le podium s'affiche avec le **Top 5 uniquement** et les **statistiques collectives du groupe** (Moyenne de la classe, Pourcentage global de bonnes réponses). L'ensemble tient dans 100% de la hauteur de l'écran sans aucun défilement vertical.
   - **Sur le smartphone de chaque élève** : L'élève accède à sa restitution personnelle (son score, son rang sans stigmatisation, sa situation par rapport à la moyenne) et peut faire défiler l'intégralité du classement de la classe.
