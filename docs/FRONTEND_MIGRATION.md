# Frontend ClassSection, Subject, and Materials Migration

## Implemented Scope

The React application now uses the migrated `ClassSection`, `ClassEnrollmentRequest`, `Subject`, `ClassSubjectLink`, and Learning Materials APIs. It includes persisted JWT authentication, role-aware navigation, route guards, typed API services, and consistent loading, empty, success, and error states.

Teachers can:

- create, edit, select, and delete their class sections;
- review enrollment requests, approve or decline them, and remove members;
- create, edit, and delete subjects;
- request a subject link by its seven-character code; and
- approve or decline link requests for subjects they own;
- browse materials by approved class/subject link; and
- upload directly to MinIO in 5 MiB parts with ETag capture, progress, cancellation, and up to three attempts per transiently failed part.

Students can register without a class code, request enrollment with a six-character code, review request status, see their approved class, list its approved subjects, and download subject materials through short-lived presigned URLs.

Activities, submissions, grades, analytics, and notifications remain intentionally out of scope.

## Local Verification

Start dependencies and both applications from the repository root:

```bash
docker compose up -d
cd backend && mvn spring-boot:run
cd frontend && npm ci && npm run dev
```

Run frontend checks with `npm run lint` and `npm run build`. Vite proxies `/api` to `http://localhost:8080`; JWTs are attached only to backend API calls. Presigned part uploads go directly to MinIO without exposing credentials.

## Database Migration

Apply `migration_scripts/class_section_subject_migration.sql` to an existing development database before testing migrated accounts. The script preserves legacy data and leaves destructive legacy-table drops commented out. Review its preflight queries and take a database backup before execution; removal of old tables requires a separate, explicit cleanup decision.
