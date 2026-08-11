# APPTITLE — Setup Guide

## Option A — GitHub Codespaces (works entirely from a phone browser)

This repo includes a `.devcontainer/` config, so Codespaces auto-installs
Java 21, Node 20, and Docker — no local machine needed.

1. Push/upload this project to a GitHub repository.
2. On the repo page, tap **Code → Codespaces → Create codespace on main**.
3. Wait for the container to build (a few minutes the first time — it's
   installing Java, Node, and Docker). `postCreateCommand` then automatically
   runs `npm install` for the frontend and pre-fetches Maven dependencies.
4. `postStartCommand` automatically runs `docker compose up -d`, starting
   Postgres and MinIO inside the Codespace.
5. Open two terminals in the Codespace (VS Code's terminal panel, `+` to add
   a second one):
   ```bash
   # Terminal 1
   cd backend && mvn spring-boot:run

   # Terminal 2
   cd frontend && npm run dev
   ```
6. Codespaces detects ports 8080 and 5173 and pops up a notification —
   tap **Open in Browser** to view the app. The frontend's health-check page
   will show a green "Backend API" status once both are running.

See the end of this document for a full step-by-step phone walkthrough,
including creating the repo and uploading the project files.

---

## Option B — Local machine

## Prerequisites

- **Java 21+** (JDK) — LTS release, required by this project's `pom.xml`
- **Maven 3.9+** (or use your IDE's bundled Maven)
- **Node.js 20+** and npm
- **Docker + Docker Compose** (for Postgres and MinIO)

## 1. Start local infrastructure

From the repo root:

```bash
docker compose up -d
```

This starts:
- **PostgreSQL** on `localhost:5432` — database `apptitle`, user/password `apptitle` / `apptitle_dev_password`
- **MinIO** on `localhost:9000` (S3 API) and `localhost:9001` (web console) — credentials `apptitle_admin` / `apptitle_dev_password`

Check both are healthy:

```bash
docker compose ps
```

> MinIO buckets aren't created or used by code yet — that starts in Phase 4
> (Learning Materials). The service is running now so later phases don't
> require revisiting environment setup.

## 2. Backend

```bash
cd backend
cp .env.example .env
```

Fill in `.env` with real values before anything beyond local dev — the
defaults baked into `application.yml` match `docker-compose.yml` exactly, so
**local development works without editing `.env` at all**. The file exists
so the required variables are documented up front.

Run the backend:

```bash
# If you have Maven installed:
mvn spring-boot:run

# Or generate the wrapper once (requires network) and use it from then on:
mvn -N io.takari:maven:wrapper -Dmaven=3.9.9
./mvnw spring-boot:run
```

The backend starts on **http://localhost:8080**. On first run, Hibernate
creates the `users` table automatically (`ddl-auto: update` — see
`application.yml`; this is replaced by a real migration tool before
production).

Verify it's up:

```bash
curl http://localhost:8080/api/health
```

### Running backend tests

Tests require the Postgres container from step 1 to be running:

```bash
mvn test
```

## 3. Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

The frontend starts on **http://localhost:5173**. Open it in a browser —
the landing page calls `GET /api/health` through Vite's dev proxy (see
`vite.config.ts`) and shows whether the backend is reachable. A green dot
means the full chain (React → Vite proxy → Spring Boot → this far, DB
connectivity isn't exercised by `/api/health` itself, but Spring Boot won't
start at all if it can't reach Postgres) is working.

## 4. Environment variables reference

### Backend (`backend/.env.example`)

| Variable | Purpose | Local default |
|---|---|---|
| `DB_URL` | JDBC connection string | `jdbc:postgresql://localhost:5432/apptitle` |
| `DB_USERNAME` / `DB_PASSWORD` | Postgres credentials | `apptitle` / `apptitle_dev_password` |
| `JWT_SECRET` | Signing key for auth tokens (Phase 2+) | dev placeholder — **must** change before any shared/deployed environment |
| `JWT_EXPIRATION_MS` | Token lifetime | `86400000` (24h) |
| `STORAGE_ENDPOINT` / `STORAGE_ACCESS_KEY` / `STORAGE_SECRET_KEY` / `STORAGE_BUCKET` | MinIO/S3 connection (Phase 4+) | matches `docker-compose.yml` |
| `FRONTEND_ORIGIN` | Allowed CORS origin | `http://localhost:5173` |

### Frontend (`frontend/.env.example`)

| Variable | Purpose | Local default |
|---|---|---|
| `VITE_API_BASE_URL` | Absolute API origin | unused in dev (Vite proxy handles it); needed for Phase 10 production build |

## 5. Common issues

- **Backend fails to start with a connection refused error** — Postgres
  isn't up yet. Run `docker compose ps` and check the `postgres` service is
  `healthy`, not just `running`.
- **Frontend shows a red "Backend API" status** — the backend isn't running,
  or it's running on a different port than 8080. Check the terminal running
  `spring-boot:run` for startup errors.
- **Port already in use** — something else on your machine is using 5432,
  8080, or 5173. Stop it, or change the mapped port in `docker-compose.yml` /
  `application.yml` / `vite.config.ts` (keep all three consistent).

## 6. What's next

Phase 2 (Authentication) adds real login/registration, JWT issuance, and
role-based route protection on both ends. Nothing in this setup changes when
that lands — the scaffolding here is meant to be the last time the dev
environment itself needs attention.

---

## 7. Full phone walkthrough (GitHub Codespaces from scratch)

Everything below can be done from a phone browser (Chrome/Safari). No
computer required.

### Step 1 — Create a GitHub account (skip if you have one)

Go to `github.com`, sign up for free.

### Step 2 — Create a new repository

1. Tap the **+** icon (top right) → **New repository**.
2. Name it (e.g. `apptitle-lms`).
3. Set visibility to **Private** (recommended, since it's your school project).
4. Leave "Add a README" unchecked — you're uploading files directly.
5. Tap **Create repository**.

### Step 3 — Upload the project zip

1. On the new (empty) repo page, tap **Add file → Upload files**.
2. Tap **choose your files**, select the `apptitle-phase1.zip` you downloaded
   from this chat.
3. Scroll down, tap **Commit changes**. The zip is now in the repo, but still
   zipped — GitHub's uploader doesn't extract archives automatically.

### Step 4 — Open a temporary Codespace to unzip and commit the real files

1. Tap the green **Code** button → **Codespaces** tab → **Create codespace
   on main**.
2. This first Codespace won't have the devcontainer config active yet
   (it's still zipped up), so it opens with GitHub's default environment —
   that's fine, it still has `unzip` and `git`.
3. In the terminal at the bottom of the screen, run:
   ```bash
   unzip apptitle-phase1.zip
   mv apptitle/* apptitle/.[!.]* . 2>/dev/null
   rmdir apptitle
   rm apptitle-phase1.zip
   git add -A
   git commit -m "Add project scaffold with devcontainer config"
   git push
   ```
4. Once that finishes, delete this Codespace (Codespaces tab on
   github.com → **...** menu next to it → **Delete**) — you won't need it
   again, and it stops counting against your free hours.

### Step 5 — Open the real dev Codespace

1. Back on the repo page: **Code → Codespaces → Create codespace on main**.
2. This time GitHub detects `.devcontainer/devcontainer.json` and builds the
   real environment automatically — Java 21, Node 20, Docker. This takes a
   few minutes the first time only.
3. Once it's ready, `npm install` and Postgres/MinIO startup happen
   automatically in the background (check the terminal output).

### Step 6 — Run the app

Open two terminals (tap the `+` in the terminal panel for a second one):

```bash
# Terminal 1 — backend
cd backend
mvn spring-boot:run

# Terminal 2 — frontend
cd frontend
npm run dev
```

### Step 7 — View it

A notification pops up when port `5173` (frontend) is detected — tap
**Open in Browser**. You should see the APPTITLE status page with a green
dot next to "Backend API." That confirms frontend → backend → (Spring Boot
successfully started, meaning it also reached Postgres) is all working.

### Ongoing use

- Each time you come back, just open the repo's **Codespaces** tab and
  resume the existing one (don't recreate it — that eats your free hours
  rebuilding). Codespaces auto-stops after ~30 minutes idle, and resuming
  is fast.
- Commit and push your work regularly (`git add -A && git commit -m "..."
  && git push`) so nothing is lost if the Codespace is deleted.
- Free tier is 60 hours/month on a 2-core machine — stop the Codespace
  manually when you're done for the day if you want to conserve hours.

