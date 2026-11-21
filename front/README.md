# QueZap — Frontend

Angular 22 single-page application (Standalone Components, Zoneless Change Detection, Signals, `@ngrx/signals`, OpenNG Optimus UI v2 MIT, Tailwind CSS v4, PWA Service Worker).

[Root documentation](../README.md)

---

## 🔧 Prerequisites

- **Bun** `>= 1.3` (or Node.js `>= 22`)
- Running backend API on `http://localhost:8080` (or full dev stack via `docker compose -f ../docker-compose.dev.yml up -d`)

---

## 🚀 Quick Start

Install dependencies:
```bash
bun install
```

Start the local development server (`http://localhost:4200`, proxying `/apiv1` to the backend):
```bash
bun run start
```

---

## 🔄 OpenAPI SDK Code Generation

All HTTP calls use the generated Hey API TypeScript client and Zod schemas under `src/app/core/api-client/` (`sdk.gen.ts`, `zod.gen.ts`).

After updating backend endpoints or DTOs, regenerate `openapi.json` from `back/` and run:
```bash
bun run codegen
```

---

## 🧪 Testing & Linting

Run ESLint (TypeScript/HTML) and Stylelint (CSS):
```bash
bun run lint
bun run lint:ts:fix
bun run lint:css:fix
```

Run Jest unit tests (zoneless environment):
```bash
bun run test
# Single spec file:
bun run test -- src/app/features/admin/themes/components/theme-card/theme-card.spec.ts
```

Run Playwright end-to-end tests:
```bash
# Against Docker Compose stack (default http://localhost:8000):
bun run test:e2e -- --workers=1

# Against local Angular dev server (port 4200):
PLAYWRIGHT_BASE_URL="http://localhost:4200" bun run test:e2e -- --workers=1
```

---

## ⚙️ Runtime Configuration (Docker)

When running inside the Docker Nginx image, container environment variables are rendered to `/config.json` at startup (`docker-entrypoint.d/40-generate-config.sh`) and loaded before application bootstrap (`AppInitializer` → `Config.load()`), without rebuilding the image:

| Variable | Default | Description |
| :--- | :--- | :--- |
| `APP_NAME` | `QueZap` | Application title, branding, and copyright label |
| `APP_AUTHOR_NAME` | `Author User` | Author name displayed in the footer |
| `APP_AUTHOR_EMAIL` | `author@example.net` | Contact email linked in the footer |
| `APP_API_URL` | `/apiv1` | Backend API base path or URL |
| `APP_TURNSTILE_SITE_KEY` | *(empty)* | Optional Cloudflare Turnstile public site key (enables widget on registration & password reset) |

---

## 📦 Production Build

Compile the AOT production bundle and generate the PWA service worker manifest (`dist/quezap/browser`):
```bash
bun run build
```

Run the full frontend quality gate before committing:
```bash
bun run lint && bun run test && bun run build
```
