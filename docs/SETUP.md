# Classify setup guide

## Prerequisites

- Java 21 and Maven 3.9+
- Node.js 20+ and npm
- Docker with Docker Compose v2

## Clean-clone local setup

From the repository root, start PostgreSQL and MinIO:

```bash
docker compose up -d
docker compose ps
```

Wait until both `apptitle-postgres` and `apptitle-minio` report healthy. The
Compose credentials are local-development values only. PostgreSQL listens on
5432; MinIO uses 9000 for its API and 9001 for its console.

Start the backend in a second terminal:

```bash
cd backend
mvn spring-boot:run
```

The default `dev` profile matches Docker Compose and creates/updates the local
schema with Hibernate. It also creates the configured MinIO bucket. Verify the
API at `http://localhost:8080/api/health`.

Start the frontend in a third terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` to the backend.

Local `.env` files are optional. Spring Boot does not read `.env` by itself;
use a dotenv-aware IDE/shell or export variables before running Maven:

```bash
set -a
source backend/.env
set +a
cd backend && mvn spring-boot:run
```

Never commit either `.env` file; repository ignore rules exclude them.

## Verification

With PostgreSQL and MinIO healthy:

```bash
cd backend && mvn test
cd ../frontend && npm run lint && npm run build
```

The main manual acceptance chain is: register teacher and student; teacher
creates a Class Section and Subject; adviser requests the subject link; subject
teacher approves it; student requests section enrollment; adviser approves it.
Only then should the student see that subject and its materials, activities,
grades, progress, and announcements.

## Environment variables

Backend variables are documented in `backend/.env.example`:

| Variable | Required in production | Purpose |
|---|---:|---|
| `SPRING_PROFILES_ACTIVE=prod` | Yes | Enables schema validation and production logging. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Yes | PostgreSQL connection. |
| `JWT_SECRET` | Yes | JWT signing secret; use a long random value. |
| `JWT_EXPIRATION_MS` | No | Token lifetime; defaults to 24 hours. |
| `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` | Yes | S3-compatible object storage. |
| `MINIO_INITIALIZE_BUCKET` | No | Defaults to `false` in production. Provision the bucket separately. |
| `MINIO_PRESIGNED_EXPIRY_SECONDS` | No | Defaults to 900 seconds. |
| `MINIO_MAX_FILE_SIZE_BYTES` | No | Defaults to 500 MiB. |
| `FRONTEND_ORIGIN` | Yes | Exact allowed browser origin for CORS. |

The frontend has one optional build variable: `VITE_API_BASE_URL`. Leave it
unset when the SPA and API share an origin; otherwise set the full API base,
for example `https://api.example.com/api`. Vite variables are public and must
never contain secrets.

## Existing databases and SQL scripts

Back up an existing database before schema work. `migration_scripts/` contains
manual, ordered feature migrations:

1. `class_section_subject_migration.sql` (legacy Section conversion)
2. `learning_materials_migration.sql`
3. `activities_migration.sql` (also creates score proposals)
4. `grades_migration.sql`
5. `progress_migration.sql`
6. `announcements_notifications_migration.sql`

These scripts are not a complete fresh-schema migration chain: the original
users/teachers/students baseline is still created by Hibernate in development.
No Flyway or Liquibase dependency is installed. The production profile uses
`ddl-auto=validate`, so production startup requires a pre-provisioned complete
schema. Converting the full baseline to a versioned migration tool remains
deployment work and must be treated as an explicit architecture change.

## Production configuration check

Run with `SPRING_PROFILES_ACTIVE=prod`. That profile requires database, JWT,
CORS, and MinIO credentials from the environment and refuses to silently use
the local defaults. It disables SQL output, reduces Spring Security logging,
does not auto-create the object-storage bucket, and validates rather than
mutates the database schema.

Place TLS and a reverse proxy/load balancer in front of the API. Restrict the
MinIO console and database from public access. The repository does not include
a production deployment manifest or certificate automation.

## Common failures

- Connection refused at backend startup: wait for PostgreSQL health.
- MinIO upload CORS errors: make `FRONTEND_ORIGIN` match the browser origin and
  restart MinIO after changing it.
- Production schema validation failure: provision the complete baseline and
  apply the feature SQL in order; do not switch production to `ddl-auto=update`.
- Browser API network errors: use same-origin `/api` or set
  `VITE_API_BASE_URL` at frontend build time.
