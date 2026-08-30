# Repository Guidelines

## Project Goal & Architecture

Classify is a responsive learning management and student-progress system built as a modular monolith. Its core hierarchy is Class Section → Subject → materials, activities, grades, and progress. A teacher may act as both Class Adviser and Subject Teacher; students join one class and inherit its approved subjects. Treat `docs/SRS.md` as the requirements baseline and `docs/PROJECT_STATUS.md` as the current implementation roadmap. Learning Materials (Phase 4) is implemented; Activities (Phase 5) is next.

## Project Structure & Module Organization

The Java 21/Spring Boot backend is under `backend/src/main/java/com/apptitle`, organized by feature and then controller, service, repository, entity, and DTO. Backend configuration lives in `backend/src/main/resources`; JUnit tests mirror packages under `backend/src/test`. React/TypeScript code is in `frontend/src`, with API clients in `services/`, models in `types/`, and role-specific pages/components in their respective folders. Keep SQL changes in `migration_scripts/` and documentation in `docs/`.

## Build, Test, and Development Commands

- `docker compose up -d`: start PostgreSQL and MinIO.
- `cd backend && mvn spring-boot:run`: run the API on port 8080.
- `cd backend && mvn test`: run JUnit/Mockito tests.
- `cd backend && mvn clean package`: compile, test, and package.
- `cd frontend && npm ci`: install locked dependencies.
- `cd frontend && npm run dev`: start Vite on port 5173.
- `cd frontend && npm run lint && npm run build`: lint, type-check, and bundle.

Copy module `.env.example` files for local overrides; never commit secrets.

## Coding Style & Domain Constraints

Use four-space Java and two-space TypeScript/TSX indentation. Use `PascalCase` for Java classes, React components, and exported types; use `camelCase` for methods, variables, hooks, and functions. Keep Spring layers feature-local and use constructor injection. Preserve frontend double quotes and semicolons.

Enforce authorization in the backend. Registration and class joining remain separate; students never join subjects directly. Activity categories are exactly Written Activity, Performance Task, and Test. Preserve ownership checks and the established direct-to-MinIO multipart upload contract.

## Testing & Contributions

Name backend tests `*Test` and methods by behavior, such as `registerStudent_rejectsDuplicateLrn`. Cover success, validation, authorization, duplicate, and failure paths. Frontend changes must pass lint and production build.

Use focused, imperative commits; Conventional Commit prefixes such as `feat:` and `fix:` are encouraged. Pull requests should explain the problem and solution, list verification commands, link issues, note schema/configuration changes, and include screenshots for UI work. Back up databases and review migration preflight steps before applying SQL.
