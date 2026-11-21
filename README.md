# QueZap

Real-time interactive classroom quiz application (projector/host screen + mobile student clients synchronized via Server-Sent Events).

---

## 🚀 Production Deployment (Docker + GraalVM Native + RustFS)

[docker-compose.yml](docker-compose.yml) provides a production-ready stack:
- **Pre-built `linux/amd64` images** published to GitHub Container Registry (`ghcr.io/kwaadpepper/quezap-backend` & `ghcr.io/kwaadpepper/quezap-frontend`) on every `v*` release tag.
- **GraalVM 25 native backend** (`back/Dockerfile.native`): instant startup and low memory footprint.
- **RustFS S3-compatible object storage** with automatic public bucket creation and UID `10001` volume permission setup.
- **Persistent storage** via named Docker volumes (`quezap_postgres_data`, `quezap_rustfs_data`, `quezap_rustfs_logs`).
- **Network isolation**: only the Nginx frontend is published on the host (`${APP_PORT:-8000}`); PostgreSQL, RustFS, and the backend remain internal to `quezap-net`.
- Automatic Docker JSON log rotation (`10m` × 3 files).

### 1. Configure `.env`

Copy the template and replace every `CHANGE_ME_*` placeholder along with `APP_PUBLIC_URL` (for example, `https://quezap.example.com`):

```bash
cp .env.example .env
```

Generate strong secrets:

```bash
openssl rand -hex 32     # APP_JWT_SECRET_KEY
openssl rand -base64 24  # POSTGRES_PASSWORD and RUSTFS_SECRET_KEY
```

### 2. Pull images and start the stack

```bash
git pull
docker compose pull
docker compose up -d
```

To build locally instead of pulling from GHCR, run `docker compose up -d --build` (allow 6–8 GB of RAM for the GraalVM native compilation in `back/Dockerfile.native`, or switch `dockerfile` to `Dockerfile` to run on the JVM).

### 3. Create the first teacher account

- **Web UI**: open the application, click **Créer une partie**, and register with an email and password.
- **REST API**:
  ```bash
  curl -fsS -X POST http://127.0.0.1:8000/apiv1/users \
    -H "Content-Type: application/json" \
    -d '{"name":"Admin","email":"admin@example.com","password":"StrongPassword#2026"}'
  ```
- **Admin CLI**:
  ```bash
  docker compose --profile cli run --rm cli
  # Inside Spring Shell:
  users add --name Admin --email admin@example.com
  ```

### Exposed Ports (Production)

| Service | Host Port | Description |
| :--- | :--- | :--- |
| **Frontend** (`frontend`) | `${APP_PORT:-8000}` | Angular SPA served by Nginx (reverse proxies `/apiv1/` with SSE streaming and `/quezap-public-images/` to RustFS) |
| **Backend Native** (`backend`) | *Internal only* | GraalVM 25 native binary reachable only inside `quezap-net` |
| **Database** (`postgres`) | *Internal only* | Persistent PostgreSQL 17 (`quezap_postgres_data`) |
| **Object Storage** (`rustfs`) | *Internal only* | Persistent S3 storage (`quezap_rustfs_data`), public images served through Nginx |

> **Reverse proxy note (Traefik / Nginx / Caddy)**: disable response buffering and compression on `/apiv1/` so Server-Sent Events (SSE) stream in real time, and allow request bodies up to at least `10 MB` for image uploads.

---

## 💻 Local Development (`docker-compose.dev.yml`)

[docker-compose.dev.yml](docker-compose.dev.yml) requires no `.env` file (defaults match `back/quezap-api/src/main/resources/application.yml`) and uses a fast JVM backend image (`back/Dockerfile`):

```bash
# Option A: Start infrastructure only (to run backend via bootRun and frontend via bun run start)
docker compose -f docker-compose.dev.yml up -d postgres rustfs rustfs-init

# Option B: Start the full containerized stack in JVM mode
docker compose -f docker-compose.dev.yml up -d --build
```

### Exposed Ports (Development)

| Service | Host Port | Description |
| :--- | :--- | :--- |
| **Frontend** (`frontend`) | `http://localhost:8000` | Angular application via Nginx |
| **PostgreSQL** (`postgres`) | `5432` | Direct database access (`quezap` / `quezap_password`) |
| **RustFS S3 API** (`rustfs`) | `http://localhost:9190` | S3 API (mapped from `9000` to avoid local port conflicts) |
| **RustFS Console** (`rustfs`) | `http://localhost:9191` | RustFS admin UI (`quezap_java_writer` / `LocalPassword`) |

---

## 🌱 Seed Sample Data

```bash
# Development:
docker compose -f docker-compose.dev.yml --profile seed run --rm seed

# Production (optional demo dataset; remove or change the demo account afterwards):
docker compose --profile seed run --rm seed
```

Default admin credentials created by `seed.sh`:
- **Email**: `admin@quezap.com`
- **Password**: `Password123!`

---

## 🔧 Admin CLI

```bash
# Development:
docker compose -f docker-compose.dev.yml --profile cli run --rm cli

# Production:
docker compose --profile cli run --rm cli
```

---

## 🛑 Stop & Backup

```bash
# Stop containers while preserving persistent volumes
docker compose down

# Hot-backup PostgreSQL database
docker exec quezap-postgres pg_dump -U quezap quezap > backup_quezap_$(date +%F).sql

# Remove containers AND data volumes (irreversible)
docker compose down -v
```

---

## 🛠️ Tech Stack & Module Documentation

- **Backend**: Spring Boot 3.5.16 (Java 25 / GraalVM 25 Native), DDD & Hexagonal Architecture, Spring Security (JWT + Argon2), RustFS S3, PostgreSQL 17 → [back/README.md](back/README.md)
- **Frontend**: Angular 22 (Standalone, Zoneless, Signals), `@ngrx/signals`, OpenNG Optimus UI v2 (MIT), Tailwind CSS v4, PWA Service Worker, SSE → [front/README.md](front/README.md)

## 🌍 Self-Hosting

Self-hosted at <https://quezap.fr> using Docker Compose and a GraalVM 25 native backend.
