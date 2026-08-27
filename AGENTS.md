# Repository Guidelines

## Project Structure & Module Organization

This repository contains a Java 21/Spring Boot backend and a React/TypeScript frontend. Backend code lives under `backend/src/main/java/com/apptitle`, organized by feature (`auth`, `classsection`, `subject`, and similar) and then by controller, service, repository, entity, and DTO. Configuration files are in `backend/src/main/resources`; JUnit tests mirror the package structure under `backend/src/test`. Frontend application code is in `frontend/src`, with shared API access in `services/` and TypeScript models in `types/`. Keep database changes in `migration_scripts/` and project documentation in `docs/`.

## Build, Test, and Development Commands

- `docker compose up -d`: start PostgreSQL and MinIO for local development.
- `cd backend && mvn spring-boot:run`: run the API on port 8080.
- `cd backend && mvn test`: execute the backend JUnit/Mockito suite.
- `cd backend && mvn clean package`: compile, test, and package the backend.
- `cd frontend && npm ci`: install the locked frontend dependencies.
- `cd frontend && npm run dev`: start Vite on port 5173.
- `cd frontend && npm run lint`: run ESLint over TypeScript and React code.
- `cd frontend && npm run build`: type-check and create the production bundle.

Copy each module's `.env.example` to `.env` when local overrides are needed. Never commit secrets.

## Coding Style & Naming Conventions

Use four-space indentation in Java and two spaces in TypeScript/TSX. Java packages are lowercase; classes use `PascalCase`, methods and variables use `camelCase`, and DTOs use descriptive suffixes such as `CreateSubjectRequest` or `SubjectResponse`. Keep Spring layers feature-local and prefer constructor injection. React components and exported types use `PascalCase`; hooks and functions use `camelCase`. Follow the existing ESLint configuration and preserve the frontend's double-quote, semicolon style.

## Testing Guidelines

Use JUnit 5 and Mockito. Name test classes `*Test` and test methods after behavior, for example `registerStudent_rejectsInvalidClassroomCode`. Add service tests for success, validation, authorization, and failure paths. Start PostgreSQL before running the complete backend suite. The frontend currently has no test runner; lint and production build are required checks for frontend changes.

## Commit & Pull Request Guidelines

Recent history favors short, imperative summaries, often with Conventional Commit prefixes such as `feat:`. Keep commits focused; use forms like `fix: reject duplicate section codes`. Pull requests should explain the problem and solution, list verification commands, link relevant issues, and call out schema or configuration changes. Include screenshots for visible UI changes and update `docs/` when behavior or setup changes.
