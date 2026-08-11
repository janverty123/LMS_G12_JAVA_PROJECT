# APPTITLE — Learning Management with Student Progress Monitoring System

Full-stack LMS for teachers and students: sections, master lists, materials,
activities (Written / Performance / Periodical), scores, grade computation,
student score self-submission with approval, progress monitoring, announcements,
and notifications.

## Stack

- **Frontend:** React + TypeScript + Vite + Tailwind CSS
- **Backend:** Java 21 + Spring Boot 3.5 (Web, Security, Data JPA, Validation)
- **Database:** PostgreSQL
- **File storage:** MinIO (S3-compatible)
- **Dev orchestration:** Docker Compose (Postgres + MinIO only — app services run locally for fast iteration)

## Repository layout

```
apptitle/
├── backend/     Spring Boot application (Maven)
├── frontend/    React + TypeScript application (Vite)
├── docker-compose.yml   Postgres + MinIO for local development
└── docs/SETUP.md        Full setup instructions
```

## Quick start

See [docs/SETUP.md](./docs/SETUP.md) for full instructions. Short version:

```bash
# 1. Start Postgres + MinIO
docker compose up -d

# 2. Run the backend
cd backend
cp .env.example .env   # fill in values
./mvnw spring-boot:run

# 3. Run the frontend
cd frontend
cp .env.example .env
npm install
npm run dev
```

Backend runs on `http://localhost:8080`, frontend on `http://localhost:5173`.

## Status

**Phase 1 — Project Setup (complete):**
- Backend and frontend scaffolds created and wired together.
- PostgreSQL configured via Docker Compose and Spring datasource config.
- MinIO configured for future file-storage phases (buckets not yet used by code).
- Base entity, global exception handling, CORS, and security skeleton in place.
- Health-check endpoint (`GET /api/health`) confirms frontend ↔ backend connectivity.

See the project's Claude Project instructions for the full phased roadmap
(Phase 2 = Authentication, onward).
