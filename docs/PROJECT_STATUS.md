# Classify — Project Status & Migration Brief

**Prepared against:** *Classify — Modern Learning Management & Student Progress Monitoring System — Updated SRS and Architecture Specification.md*
**As of 2026-08-29:** Phases 1–10 are complete within the agreed deadline scope. The focused hardening pass covered authorization, critical-path tests, mobile layout smoke fixes, setup/migration documentation, and production configuration checks; database migrations remain pending per environment.
**Purpose of this document:** this is written to be handed to a coding agent as an implementation brief. Section 5 in particular is meant to be followed literally, in order, not just read for context.

---

## 1. Current Status

The application is functional through **Announcements and Notifications** — Java 21, Spring Boot 3.5.16, PostgreSQL, MinIO, GitHub Codespaces. The core LMS workflows plus persistent, user-scoped notifications and section announcements are implemented.

**Three architecture decisions are now implemented** (see Section 4):

1. The former combined `Section` entity is split into **`ClassSection` + `Subject` + a linking table**, matching the SRS's two-tier model.
2. The **Class Adviser / Subject Teacher distinction** is reflected through entity ownership and separate API workflows.
3. **Registration must NOT require a classroom code.** Students register first; joining a class section (by entering its code) happens as a separate step afterward.

Existing databases created with the legacy model must run `migration_scripts/class_section_subject_migration.sql` after taking a backup. The script preserves legacy tables for manual rollback inspection.

---

## 2. Completed

### Phase 1 — Foundation
*(SRS §37 Docker Compose, §38 Tech Stack, §41–42 Architecture)*
- Spring Boot + React/TypeScript/Vite/Tailwind scaffolds, wired together.
- PostgreSQL + MinIO via Docker Compose; MinIO bucket/client configuration is now used by Phase 4.
- Codespaces devcontainer (Java 21, Node 20, Docker-in-Docker).
- Global exception handling, CORS, health check.

### Phase 2 — Authentication
*(SRS §4 Registration, §5 Authentication, §48 Security)*
- Teacher registration (name, email, password).
- Student registration (name, email, password, LRN); class joining is a separate authenticated workflow.
- Spring Security, BCrypt, stateless JWT, role-based authorization.

### Phase 3 — Class Section, Enrollment & Subject Management
- Teacher creates a `ClassSection` with an auto-generated six-character code.
- Student sends a join request; teacher approves/declines.
- Teacher: section CRUD, pending-request review, member list, member removal.
- Subject Teacher: subject CRUD and subject-link request approval/decline.
- Student: view their own enrollment requests, one approved Class Section, and inherited approved Subjects.
- Ownership checks enforced and unit-tested.

### Phase 4 — Learning Materials *(implemented)*
- Persistent `LearningMaterial` and `MaterialChunk` metadata.
- Five MiB MinIO multipart initialization with server-generated object keys and presigned part URLs.
- ETag-based multipart completion and short-lived presigned downloads.
- Subject-owner upload authorization and approved-enrollment student read access.
- Backend services, controllers, migration SQL, and service tests implemented.
- Teacher and student material screens, direct browser-to-MinIO chunk uploads, ETag capture, progress, cancellation, and transient-failure retry implemented.

### Phase 5 — Activities *(implemented)*
- Persistent activities with exactly Written Activity, Performance Task, and Test categories.
- Subject-owner create, update, delete, and list APIs; approved-enrollment student read access.
- Teacher and student activity screens, migration SQL, and service tests implemented.
- Optional teacher attachments and student work submissions reuse the shared five MiB multipart uploader.
- Submission roster, `NOT_SUBMITTED` / `SUBMITTED` / `LATE` / `GRADED` states, downloads, resubmission, and teacher score entry/editing implemented.

### Phase 6 — Grades *(implemented)*
- Per-Class/Subject grading weights for Written Activity, Performance Task, and Test, validated to total exactly 100%.
- Backend-authoritative category averages and weighted final grades from graded submissions.
- Teacher gradebook and student grade view implemented.
- Server-generated `.xlsx` gradebook export implemented with Apache POI.

### Phase 7 — Student Self-Submitted Scores *(implemented)*
- Per-activity opt-in controls whether students may propose scores.
- Reported score proof photos reuse the shared five MiB presigned multipart uploader.
- `PENDING`, `APPROVED`, `REJECTED`, and `EDITED_APPROVED` review states implemented.
- Only teacher approval writes the authoritative graded submission score; pending and rejected proposals are excluded from grade calculations.
- Student and teacher review screens, ownership enforcement, migration SQL, and service tests implemented.

### Phase 8 — Progress Monitoring *(implemented)*
- Completion percentage and missing activities are calculated from actual student work submissions.
- Current grade is sourced from the backend-authoritative weighted gradebook.
- Teachers explicitly configure the On Track and Needs Attention boundaries per Class/Subject; no status thresholds are silently invented.
- Teacher dashboard and student progress view expose completion, missing work, current grade, and status.

### Phase 9 — Announcements & Notifications *(implemented)*
- Class advisers can post, edit, and delete section announcements; only approved section members can read them.
- PostgreSQL notification records are authoritative, user-scoped, and support read/unread state.
- Triggers cover announcements, materials, activities, upcoming deadlines, score requests, released grades, and join-request lifecycle events.
- WebSocket delivery was intentionally omitted because real-time transport is optional.

### Phase 10 — Focused Hardening *(implemented within agreed scope)*
- Reviewed every authenticated feature endpoint and documented its adviser,
  Subject-owner, approved-enrollment, approved-link, or current-user boundary.
- Closed cross-student access to score-proof downloads and added critical-path
  authorization/calculation tests.
- Hardened mobile navigation, class tabs, file inputs, and narrow form controls.
- Updated clean-clone setup, migration limitations, environment variables, and
  production-profile configuration. Exhaustive security/performance/deployment
  certification remains explicitly out of scope.

---

## 3. Implementation Roadmap

| Order | Scope | Status | SRS reference |
|---|---|---|---|
| 1 | Foundation | Complete | §37–42 |
| 2 | Authentication | Complete | §4–5, §48 |
| 3 | Class Section, Enrollment & Subject Management | Complete | §6–14 |
| 4 | Learning Materials | Complete | §15, §27–36 |
| 5 | Activities — Written Activity / Performance Task / Test (exactly these 3) | Complete | §16–19 |
| 6 | Grades — scoring, grading-weight config, category averages, final grade, Excel export | Complete | §22–25 |
| 7 | Student self-submitted scores + proof photo, approve/reject/edit | Complete | §20–21 |
| 8 | Progress monitoring — completion %, missing activities, status | Complete | §26 |
| 9 | Announcements & notifications | Complete | §50 |
| 10 | Focused security review, critical tests, responsive smoke fixes, setup/production docs | Complete (agreed scope) | §48, §52, §58, §61 |

---

## 4. Confirmed Decisions

### 4.1 — CONFIRMED: Split into ClassSection + Subject + linking table
Per SRS §3/§6–14: a **Class Section** (owned by a Class Adviser: Grade Level, Section, School Year, 6-char Class Code, has a student roster) is a separate entity from a **Subject** (owned by a Subject Teacher: Subject Name, 7-char Subject Code). They connect via a **link** that itself goes through a request/approval step, initiated by the Class Adviser, approved by the Subject Teacher. A student joins a Class Section once; they automatically gain access to every Subject currently (and later) linked to it.

### 4.2 — CONFIRMED: Class Adviser / Subject Teacher distinction matters
A Teacher account is not itself split into two roles (still one `TEACHER` user role for auth purposes) — but the **data model and API surface** must reflect the two capacities a Teacher can hold, per SRS §3.1 and §43:
- **As Class Adviser:** owns `ClassSection`s, manages the roster, approves student join requests, initiates subject-link requests.
- **As Subject Teacher:** owns `Subject`s, approves incoming link requests from Class Advisers, will own Materials/Activities/Grades against a `Subject` in later phases.
- A single Teacher can simultaneously be the adviser of some sections AND the subject teacher of some subjects — both capacities coexist on one account, distinguished by which entities they own, not by a role flag.

### 4.3 — CONFIRMED: Registration does not require a classroom code
Per SRS §4.2/§9: registration and class-joining are two separate steps. `RegisterStudentRequest` loses its `classroomCode` field entirely. A newly registered student has no class access until they separately submit a join request using a Class Section's code.

### 4.4 — Implemented activity categories
Activity categories are exactly three — Written Activity, Performance Task, Test (§16). No additional categories are supported.

### 4.5 — Implemented upload contract
File uploads (§27–36) use 5 MiB client chunks, server-generated presigned MinIO URLs, direct browser-to-MinIO uploads, ETag capture, a maximum of three attempts per part, and a completion endpoint that finalizes the multipart upload.

---

## 5. Historical Migration Plan — ClassSection / Subject Split

This completed migration plan is retained as a historical record. Existing package/class names referenced below are real, current names in the codebase (`com.apptitle.*`).

### 5.1 Entities to add

**`ClassSection`** (`com.apptitle.classsection.entity`) — replaces `Section` for the roster/adviser side
- `id`, `adviser` (→ `Teacher`, many-to-one), `gradeLevel` (String), `section` (String), `schoolYear` (String), `classCode` (String, unique, 6 chars — reuse the existing `ClassCodeGenerator` pattern, adjusted to 6 chars per SRS)

**`Subject`** (`com.apptitle.subject.entity`) — new
- `id`, `subjectTeacher` (→ `Teacher`, many-to-one), `subjectName` (String), `subjectCode` (String, unique, 7 chars — same generator pattern, 7 chars per SRS)

**`ClassSubjectLink`** (`com.apptitle.subject.entity` or its own `classsubjectlink` package)
- `id`, `classSection` (→ `ClassSection`), `subject` (→ `Subject`), `status` (enum: `PENDING`/`APPROVED`/`DECLINED`)
- Unique constraint on (`classSection`, `subject`) — no duplicate links.

**`ClassEnrollmentRequest`** — rename/repurpose the existing `JoinRequest` entity to point at `ClassSection` instead of the old `Section`
- `id`, `student` (→ `Student`), `classSection` (→ `ClassSection`), `status` (enum, same as before)
- Unique constraint on (`student`, `classSection`).

### 5.2 Entities/code to remove
- `com.apptitle.section.entity.Section` and its repository/service/controller/DTOs — replaced by `ClassSection`.
- The old `JoinRequest` entity's direct reference to `Section` — repointed to `ClassSection` (rename the class if that's cleaner, or keep the name and just change the FK — agent's call, but keep it consistent with the rest of the codebase's naming).

### 5.3 Auth changes
- `RegisterStudentRequest`: remove `classroomCode` field entirely.
- `AuthService.registerStudent`: remove all `Section`/`JoinRequest` creation logic — registration becomes exactly what it was before the classroom-code requirement was added (unique email + unique LRN, nothing else).
- `AuthResponse`: remove `sectionName`/`joinRequestStatus` fields (or leave nullable but never populate them at registration — agent's call; simplest is to remove them and revert to the plain 5-field response, since nothing sets them anymore).
- **Constraint from memory: do not reintroduce the `UsernamePasswordAuthenticationToken` import bug** — it must come from `org.springframework.security.authentication`, not `.web.authentication`, if this file is touched.

### 5.4 New REST endpoints

**Class Sections (Class Adviser capacity):**
```
POST   /api/teacher/class-sections                              create (gradeLevel, section, schoolYear) -> returns classCode
GET    /api/teacher/class-sections                               list own
PUT    /api/teacher/class-sections/{id}                          edit (not classCode)
DELETE /api/teacher/class-sections/{id}                          delete (cascade: enrollment requests + subject links)
GET    /api/teacher/class-sections/{id}/students                 roster (approved only)
GET    /api/teacher/class-sections/{id}/join-requests             pending enrollment requests
PUT    /api/teacher/class-enrollment-requests/{id}/approve
PUT    /api/teacher/class-enrollment-requests/{id}/decline
DELETE /api/teacher/class-sections/{id}/students/{studentId}      remove from roster
```

**Subjects (Subject Teacher capacity):**
```
POST   /api/teacher/subjects                                     create (subjectName) -> returns subjectCode
GET    /api/teacher/subjects                                     list own
PUT    /api/teacher/subjects/{id}
DELETE /api/teacher/subjects/{id}
GET    /api/teacher/subjects/{id}/link-requests                  pending ClassSubjectLink requests
PUT    /api/teacher/subject-links/{id}/approve
PUT    /api/teacher/subject-links/{id}/decline
```

**Linking (Class Adviser initiates):**
```
POST   /api/teacher/class-sections/{classSectionId}/subject-links    body: { subjectCode } -> creates PENDING ClassSubjectLink
```

**Student:**
```
POST   /api/students/me/class-join-requests                      body: { classCode } -> creates PENDING ClassEnrollmentRequest
GET    /api/students/me/class-join-requests                      own requests, any status
GET    /api/students/me/class-section                             their one approved class (SRS: a student belongs to exactly one Class Section)
GET    /api/students/me/subjects                                  all Subjects linked (APPROVED) to their approved Class Section
```

### 5.5 Ownership check pattern (carry over from existing code)
Every teacher-facing endpoint must verify the acting teacher actually owns the `ClassSection`/`Subject`/request in question (same pattern as the existing `JoinRequestService`/`SectionService` — resolve `Teacher` from the authenticated email, compare `.getId()` against the owning FK, throw `ApiException.forbidden(...)` on mismatch). Keep this pattern; it's already established and tested.

### 5.6 Build constraints (from project memory — do not regress these)
- `backend/pom.xml` must keep the explicit `maven-compiler-plugin` → `annotationProcessorPaths` entry wiring Lombok via `${lombok.version}`. Do not remove it or fall back to default classpath-scanning.
- Any Spring Security `UsernamePasswordAuthenticationToken` usage imports from `org.springframework.security.authentication`, never `.web.authentication`.

### 5.7 Tests to update/add
- Update/replace `AuthServiceTest`'s classroom-code-related test cases (they test behavior being removed in 5.3).
- New service tests mirroring the existing `JoinRequestServiceTest` pattern, covering: subject-link approve/decline ownership checks, class-enrollment approve/decline ownership checks, duplicate-link prevention, duplicate-enrollment-request prevention.

### 5.8 Acceptance criteria (what "done" looks like)
A teacher can: register → create a Class Section → create a Subject → link the Subject to the Class Section (as if a different teacher, or the same one, per SRS this can be the same account) → approve the link. A student can: register (no code needed) → send a class-join-request using the Class Section's code → get approved by the adviser → then see that Subject in `GET /api/students/me/subjects` **without ever directly requesting the subject** — access is inherited from the approved Class Section, per SRS §9/§12.

---

## 6. Additive scope (not in SRS, added by the team)
- Excel/spreadsheet **import** for the grading system, in addition to the SRS's export-only requirement (§25). Shape not yet defined — scope this when Phase 6 (Grades) starts.

---

## 7. Next Step
Back up and migrate any legacy development database, verify the Class Section / Subject and Learning Materials acceptance flows, then scope Phase 5 Activities separately. Later modules remain unimplemented.
