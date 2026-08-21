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

**Phase 2 — Authentication (complete, reworked twice):**
- Teacher registration (`POST /api/auth/register/teacher`) and login —
  unaffected by everything below.
- Student registration (`POST /api/auth/register/student`) — name, LRN,
  email, password, **and a required classroom code**. LRN is stored (Philippine
  DepEd record-keeping) but verifies nothing. The classroom code must match
  a real `Section`, but only proves the classroom exists — it doesn't
  verify anything about the student. A valid registration creates the
  account **and** a `PENDING` `JoinRequest` for that section in the same
  step; the student does not have real access until a teacher approves it.
- JWT issuance/validation, BCrypt password hashing, stateless Spring Security
  filter chain protecting everything except `/api/health` and `/api/auth/**`.
- Unit tests covering registration, classroom-code validation, duplicate
  email/LRN handling, and login (`AuthServiceTest`).

**Minimal Section support (pulled forward from Phase 3, teacher-only):**
- `POST /api/teacher/sections` — create a classroom, get back its join code.
- `GET /api/teacher/sections` — list your own classrooms.
- Intentionally narrow: no edit/delete, no pending-request review, no member
  management yet — that's the real Phase 3 scope, building on the same
  `Section`/`JoinRequest` entities already in place.

> **History of this model:** originally specced as a teacher-maintained
> "master list" that auto-matched and auto-enrolled students at registration.
> Per group decision, replaced with a Google Classroom-style flow: one
> classroom per teacher per section+subject, identified by a typed join
> code, with a request-to-join step a teacher must approve — never instant,
> unlike default Google Classroom behavior. `MasterListEntry` and the old
> auto-`Enrollment` entity were removed entirely; `JoinRequest` is the
> current mechanism.

**Phase 3 — Section & Join Request management (next up):**
- Teacher: edit/delete sections, view pending join requests, approve/decline,
  view and remove current members.
- Student: view their own join requests (pending/approved/declined) and
  their list of approved classrooms.

See the project's Claude Project instructions for the full phased roadmap.



