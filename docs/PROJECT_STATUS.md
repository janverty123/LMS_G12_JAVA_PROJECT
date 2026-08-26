# APPTITLE (Classify) — Project Status & Migration Brief

**Prepared against:** *Classify — Modern Learning Management & Student Progress Monitoring System — Updated SRS and Architecture Specification.md*
**As of:** End of Phase 3 (Sections & Join Requests), decisions confirmed for the Class Section / Subject migration
**Purpose of this document:** this is written to be handed to a coding agent as an implementation brief. Section 5 in particular is meant to be followed literally, in order, not just read for context.

---

## 1. Current Status

The backend is functional through **Phase 3** of an 11-phase build plan — Java 21, Spring Boot 3.5.16, PostgreSQL, MinIO, GitHub Codespaces. Authentication and the classroom join-request workflow are built and verified end-to-end.

**Three architecture decisions have now been confirmed** (previously open questions, now settled — see Section 4):

1. Split the current combined `Section` entity into **`ClassSection` + `Subject` + a linking table**, matching the SRS's two-tier model.
2. The **Class Adviser / Subject Teacher distinction matters** and should be reflected in the data model and API surface.
3. **Registration must NOT require a classroom code.** Students register first; joining a class section (by entering its code) happens as a separate step afterward.

**This means a migration is required before Phase 4 (Materials) starts.** Building Materials/Activities/Grades against the current single-`Section` model and reconciling later would be far more expensive than migrating now, before those phases add more code on top of the wrong shape. Section 5 below is the full technical plan for that migration.

---

## 2. Completed

### Phase 1 — Foundation
*(SRS §37 Docker Compose, §38 Tech Stack, §41–42 Architecture)*
- Spring Boot + React/TypeScript/Vite/Tailwind scaffolds, wired together.
- PostgreSQL + MinIO via Docker Compose (MinIO not yet used by code).
- Codespaces devcontainer (Java 21, Node 20, Docker-in-Docker).
- Global exception handling, CORS, health check.

### Phase 2 — Authentication
*(SRS §4 Registration, §5 Authentication, §48 Security)*
- Teacher registration (name, email, password).
- Student registration (name, email, password, LRN) — **currently also requires a classroom code; this requirement is being removed per Section 5 below.**
- Spring Security, BCrypt, stateless JWT, role-based authorization.

### Phase 3 — Section & Join Request Management *(being migrated — see Section 5)*
- Teacher creates a classroom (`Section`) with an auto-generated join code.
- Student sends a join request; teacher approves/declines.
- Teacher: section CRUD, pending-request review, member list, member removal.
- Student: view own join requests and approved classes; join additional classrooms after registration.
- Ownership checks enforced and unit-tested.
- **All of the above needs to be re-homed onto the new `ClassSection`/`Subject`/link model — see Section 5 for exactly what carries over vs. what's new.**

---

## 3. Needed

| Order | Scope | SRS reference |
|---|---|---|
| **Next** | **Migration: split `Section` into `ClassSection` + `Subject` + linking table** (Section 5) | §3, §6–14 |
| 4 | Learning Materials — MinIO resumable multipart upload, student read access | §15, §27–36 |
| 5 | Activities — Written Activity / Performance Task / Test (exactly these 3) | §16–19 |
| 6 | Grades — scoring, grading-weight config, category averages, final grade, Excel export | §22–25 |
| 7 | Student self-submitted scores + proof photo, approve/reject/edit | §20–21 |
| 8 | Progress monitoring — completion %, missing activities, status | §26 |
| 9 | Announcements & notifications | §50 |
| 10 | Security review, test coverage, responsive UI, deployment docs | §48, §52, §58, §61 |

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

### 4.4 — Not yet built, locked by spec
Grading categories are exactly three — Written Activity, Performance Task, Test (§16). No additional categories when Phase 5 starts.

### 4.5 — Not yet built, heavily specified
File uploads (§27–36): 5MB client chunks, server-generated presigned MinIO URLs, browser uploads directly to MinIO, ETag capture, retry ×3, completion endpoint finalizes the multipart upload. Phase 4 must follow this shape, not a simpler single-request upload.

---

## 5. Migration Plan — ClassSection / Subject Split

This section is the literal task list. Existing package/class names referenced below are real, current names in the codebase (`com.apptitle.*`).

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
Execute Section 5 in full before starting Phase 4. Phase 4 (Materials) is specified against the Class+Subject scope (SRS §14) and should be built directly on the new model, not on the old `Section` entity.
