# Authorization review — 2026-08-29

All routes except health and authentication require a valid JWT. Controllers
role-gate teacher and student APIs; services independently resolve the current
profile and enforce ownership. IDs from request paths are never sufficient for
access by themselves.

| Area | Teacher authorization | Student authorization |
|---|---|---|
| Class Sections and enrollment | Section adviser owns CRUD, roster, and request decisions. | Student can create/list only their requests and read their approved section. |
| Subjects and links | Subject teacher owns Subject CRUD and link decisions; section adviser initiates and lists section links. | Subjects are inherited only through approved enrollment plus approved link. |
| Materials | Approved link's Subject owner initializes/lists/downloads; completion also requires original uploader ownership. | Approved enrollment in material's section plus approved link; completed files only. |
| Activities | Approved link's Subject owner manages activities. | Approved enrollment in activity section plus approved link. |
| Activity files/submissions | Subject owner manages attachments, roster, downloads, and scores. | Same enrollment/link chain; own submission and every student-owned file are owner-only. Teacher attachments remain readable. |
| Grades | Approved link's Subject owner configures, reads, and exports gradebook. | Same enrollment/link chain; response is restricted to current student. |
| Score proposals | Activity Subject owner lists/reviews proposals and writes the authoritative score. | Same enrollment/link chain plus activity opt-in; student can only propose for self. |
| Progress | Approved link's Subject owner configures and views dashboard. | Same enrollment/link chain; response is restricted to current student. |
| Announcements | Section adviser creates, lists, edits, and deletes. | Only approved section members list announcements. |
| Notifications | Any authenticated user lists/marks only records whose recipient is their own User ID. | Same current-user restriction. |

The review fixed one gap: a student with access to the same activity could have
requested another student's score-proof download. Downloads now reject every
student-owned file whose owner differs from the current student. Targeted tests
cover submission files, proof files, declined subject links, gradebook/export
ownership, and cross-teacher score approval.
