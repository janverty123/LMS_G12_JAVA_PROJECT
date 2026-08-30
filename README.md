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
mvn spring-boot:run

# 3. Run the frontend
cd frontend
cp .env.example .env
npm ci
npm run dev
```

Backend runs on `http://localhost:8080`, frontend on `http://localhost:5173`.

## Status

Phases 1–9 are implemented: authentication; ClassSection/Subject enrollment;
multipart learning materials; activities, attachments and submissions; grades
and XLSX export; student score proposals with teacher approval; progress;
announcements; and persistent notifications. Phase 10 is a focused hardening
pass. See [docs/PROJECT_STATUS.md](./docs/PROJECT_STATUS.md) for exact status and
[docs/SETUP.md](./docs/SETUP.md) for clean-clone and production configuration.


