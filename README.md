# QueZap

Le jeu de Quizz pour mon amoureuse et ses élèves.

## 🚀 Démarrage rapide avec Docker Compose

L'application peut être lancée facilement avec Docker Compose dans un environnement isolé :

```bash
docker compose up -d --build
```

### 🌐 Ports exposés

Pour respecter la sécurité et l'isolation des composants, **seuls la BDD et le Frontend sont exposés sur la machine hôte** :

| Service | Port hôte | Description |
| :--- | :--- | :--- |
| **Frontend** (`frontend`) | `http://localhost:8000` | Application Angular servie par Nginx (avec reverse proxy pour l'API REST `/apiv1/` et les images MinIO `/quezap-public-images/`) |
| **BDD** (`postgres`) | `5432` | PostgreSQL 17 (accès local direct via psql, DBeaver, etc.) |
| **Backend** (`backend`) | *Non exposé* | Accessible uniquement au sein du réseau Docker dédié `quezap-net` via Nginx |
| **MinIO** (`minio`) | *Non exposé* | Stockage d'images S3 interne, images servies publiquement via le reverse proxy Frontend |

### 🔒 Réseau dédié

Tous les conteneurs communiquent au sein d'un réseau bridge dédié isolé nommé `quezap-net`.

### 🌱 Peupler la base de données (Seeding)

Pour injecter automatiquement un compte administrateur, des thèmes, des questions et une session de quiz active :

```bash
# Option 1 : Via Docker Compose (profil seed)
docker compose --profile seed run --rm seed

# Option 2 : Via le script hôte (passe par le port 8000 du front)
./seed.sh
```

Identifiants admin créés :
- **Identifiant** : `admin@quezap.com`
- **Mot de passe** : `Password123!`

### 💻 CLI d'administration

Pour lancer le shell interactif d'administration Spring Shell :

```bash
docker compose --profile cli run --rm cli
```

### 🛑 Arrêt des conteneurs

```bash
docker compose down
```

Pour supprimer également les volumes de données (BDD et MinIO) :

```bash
docker compose down -v
```

---

## 🛠️ Stack technique

- **Backend** : Spring Boot 3.5+ (Java 25), architecture hexagonale & DDD, Spring Security, S3 (MinIO), PostgreSQL.
- **Frontend** : Angular 21, Tailwind CSS, Nginx, PWA / Service Worker, SSE (Server-Sent Events).

Documentation détaillée du backend : [back/README.md](back/README.md)

## 🌍 Hébergement

Auto-hébergé sur <https://quezap.fr>.
La version du backend exécutée est compilée nativement avec GraalVM et fonctionne en tant que service systemd servi au travers de SSLH <-> Apache (reverse proxy) <-> Backend.
