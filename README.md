# QueZap

Le jeu de Quizz pour mon amoureuse et ses élèves.

---

## 🚀 Déploiement Production (NUC / Proxmox + Docker + GraalVM Native)

Le fichier [docker-compose.yml](docker-compose.yml) est prêt pour la production :
- Backend compilé en **binaire natif GraalVM 25** (`back/Dockerfile.native`) : démarrage instantané et faible empreinte RAM.
- Stockage objet **RustFS** compatible S3 (remplace MinIO) avec initialisation automatique du bucket public et gestion des permissions UID `10001`.
- **Persistance** des données via les volumes nommés `quezap_postgres_data`, `quezap_rustfs_data` et `quezap_rustfs_logs`.
- **Isolation réseau** : seul le frontend Nginx est exposé sur l'hôte (`${APP_PORT:-8000}`), PostgreSQL et RustFS restent privés dans `quezap-net`.
- Rotation automatique des logs Docker (`10 Mo` × 3 fichiers).

### 1. Préparer le fichier `.env`

```bash
cp .env.example .env
```

Éditer `.env` en remplaçant les secrets (`CHANGE_ME_*`) et `APP_PUBLIC_URL` (ex. `https://quezap.fr`).
Pour générer des secrets robustes :

```bash
openssl rand -hex 32     # pour APP_JWT_SECRET_KEY
openssl rand -base64 24  # pour POSTGRES_PASSWORD et RUSTFS_SECRET_KEY
```

### 2. Télécharger les images pré-compilées et démarrer la stack

Les images `linux/amd64` (backend natif GraalVM 25 + frontend Angular 22) sont publiées automatiquement sur GitHub Container Registry (`ghcr.io/kwaadpepper/quezap-*`) à chaque tag `v*` :

```bash
git pull
docker compose pull
docker compose up -d
```

> **Note** : Pour recompiler localement au lieu d'utiliser les images GHCR, lance `docker compose up -d --build` (prévoir ~4 à 8 Go de RAM pour la compilation native GraalVM dans `back/Dockerfile.native`). Une fois compilée, l'image finale repose sur `oraclelinux:9-slim` et ne contient plus ni JDK ni Gradle.

### 🌐 Ports exposés en production

| Service | Port hôte | Description |
| :--- | :--- | :--- |
| **Frontend** (`frontend`) | `${APP_PORT:-8000}` | Application Angular servie par Nginx (reverse proxy `/apiv1/` + SSE + images RustFS `/quezap-public-images/`) |
| **Backend Native** (`backend`) | *Non exposé* | Binaire natif GraalVM 25 accessible uniquement dans `quezap-net` |
| **BDD** (`postgres`) | *Non exposé* | PostgreSQL 17 persistant (`quezap_postgres_data`) |
| **RustFS** (`rustfs`) | *Non exposé* | Stockage S3 persistant (`quezap_rustfs_data`), images servies via Nginx |

---

## 💻 Développement local (`docker-compose.dev.yml`)

Le fichier [docker-compose.dev.yml](docker-compose.dev.yml) fonctionne sans `.env` (valeurs par défaut alignées sur `application.yml`) et utilise un build JVM rapide (`back/Dockerfile`) :

```bash
# Option A : Lancer uniquement PostgreSQL + RustFS (pour dev local bootRun + bun run start)
docker compose -f docker-compose.dev.yml up -d postgres rustfs rustfs-init

# Option B : Lancer toute la stack conteneurisée en mode JVM
docker compose -f docker-compose.dev.yml up -d --build
```

### Ports exposés en développement

| Service | Port hôte | Description |
| :--- | :--- | :--- |
| **Frontend** (`frontend`) | `http://localhost:8000` | Application Angular via Nginx |
| **PostgreSQL** (`postgres`) | `5432` | Accès local direct (`quezap` / `quezap_password`) |
| **RustFS API S3** (`rustfs`) | `http://localhost:9190` | API S3 (évite les conflits sur le port `9000`) |
| **RustFS Console** (`rustfs`) | `http://localhost:9191` | Interface web d'administration RustFS (`quezap_java_writer` / `LocalPassword`) |

---

## 🌱 Peupler la base de données (Seeding)

```bash
# En développement :
docker compose -f docker-compose.dev.yml --profile seed run --rm seed

# En production (si souhaité pour un premier jeu de démonstration) :
docker compose --profile seed run --rm seed
```

Identifiants admin créés par `seed.sh` :
- **Identifiant** : `admin@quezap.com`
- **Mot de passe** : `Password123!`

## 🔧 CLI d'administration

```bash
# En développement :
docker compose -f docker-compose.dev.yml --profile cli run --rm cli

# En production :
docker compose --profile cli run --rm cli
```

## 🛑 Arrêt & Sauvegarde

```bash
# Arrêter les conteneurs en conservant les données persistantes
docker compose down

# Sauvegarder la base PostgreSQL à chaud
docker exec quezap-postgres pg_dump -U quezap quezap > backup_quezap_$(date +%F).sql

# Supprimer conteneurs ET volumes de données (irréversible)
docker compose down -v
```

---

## 🛠️ Stack technique

- **Backend** : Spring Boot 3.5+ (Java 25 / GraalVM 25 Native), architecture hexagonale & DDD, Spring Security, S3 (RustFS), PostgreSQL 17.
- **Frontend** : Angular 22, Tailwind CSS v4, Nginx, PWA / Service Worker, SSE (Server-Sent Events).

Documentation détaillée du backend : [back/README.md](back/README.md)

## 🌍 Hébergement

Auto-hébergé sur <https://quezap.fr> (Docker + backend compilé nativement avec GraalVM 25).
