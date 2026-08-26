# Classify — Modern Learning Management & Student Progress Monitoring System

## 1. Project Overview

Build a full-stack, responsive Learning Management System (LMS) named **Classify**.

The application is inspired by Google Classroom but is specifically designed around a structured **Class Section → Subject → Learning Materials / Requirements / Progress / Gradebook** hierarchy.

The system centralizes:

- Class section management
- Student enrollment
- Subject management
- Learning materials
- Written activities
- Performance tasks
- Periodical tests
- Student submissions
- Teacher grading
- Automated grade calculation
- Student progress monitoring
- Announcements
- Notifications
- Resumable file uploads
- Excel gradebook export

The application must be usable on:

- Desktop
- Laptop
- Tablet
- Mobile browsers

The project must be implemented as a **modular monolith** rather than microservices.

Treat this document as the authoritative baseline specification. Do not remove or substantially alter a requirement unless explicitly instructed to do so.

---

# 2. Core Domain Model

The fundamental hierarchy of the application is:

```text
User
│
├── Teacher
│   │
│   ├── Class Adviser
│   │   └── Class Sections
│   │       ├── Students
│   │       └── Linked Subjects
│   │
│   └── Subject Teacher
│       └── Subjects
│           └── Linked Class Sections
│
└── Student
    └── Class Section
        └── Subjects
            ├── Materials
            ├── Requirements
            ├── Progress
            └── Student Record
```

A teacher may simultaneously act as:

1. **Class Adviser**
2. **Subject Teacher**
3. Both

Do not create separate user accounts for these teacher responsibilities.

The same Teacher account may manage classes as an adviser and subjects as a subject teacher.

---

# 3. User Roles

## 3.1 Teacher

A Teacher can perform two distinct functions.

### Class Adviser

A Class Adviser is responsible for:

- Creating class sections
- Managing the class roster
- Approving student join requests
- Viewing enrolled students
- Linking existing subjects to their class
- Sending subject join requests to Subject Teachers
- Viewing subjects linked to their class

### Subject Teacher

A Subject Teacher is responsible for:

- Creating subjects
- Managing subjects they own
- Approving class-link requests from Class Advisers
- Managing learning materials
- Creating requirements
- Managing student submissions
- Recording and reviewing scores
- Configuring grading percentages
- Viewing student progress
- Viewing the gradebook
- Exporting grades

A Teacher can be both an Adviser and Subject Teacher.

---

## 3.2 Student

A Student:

- Registers an account
- Joins a Class Section using its 6-character Class Code
- Waits for Adviser approval
- Belongs to one active Class Section
- Automatically receives access to Subjects linked to that Class Section
- Views learning materials
- Views requirements
- Submits required work
- May submit self-reported scores when enabled
- Uploads score-proof photographs when applicable
- Views progress
- Views grades
- Receives announcements and notifications

A student must not manually join individual subjects.

Subject access is inherited from the student's approved Class Section.

---

# 4. Authentication and Registration

## 4.1 Teacher Registration

Required fields:

```text
Full Name
Email Address
Password
Teacher Professional ID
```

Requirements:

- Email must be unique.
- Teacher Professional ID is stored as a string.
- Teacher Professional ID is reserved for future administrative verification.
- Passwords must never be stored in plaintext.

---

## 4.2 Student Registration

Required fields:

```text
Full Name
Email Address
Password
Learner Reference Number (LRN)
```

Requirements:

- Email must be unique.
- LRN must be unique.
- LRN is stored as a string, not an integer.
- Passwords must be securely hashed.
- Student registration does not automatically grant class access.

After registration, the student must join a class using a Class Code.

---

# 5. Authentication

Use:

- Spring Security
- Secure password hashing
- Role-based authorization
- Protected REST endpoints
- Backend authorization checks

Supported roles:

```text
ROLE_TEACHER
ROLE_STUDENT
```

The frontend must never be treated as the source of truth for authorization.

---

# 6. Class Management — Class Adviser

## 6.1 Class Creation

A Teacher can create a Class Section.

Required inputs:

```text
Grade Level
Section
School Year
```

Example:

```text
Grade Level: Grade 10
Section: Einstein
School Year: 2026–2027
```

Upon creation, the backend generates a unique:

```text
6-character alphanumeric Class Code
```

Example:

```text
A7K2P9
```

The code must:

- Be generated server-side
- Be unique
- Be case-insensitive or normalized consistently
- Not expose database IDs
- Be safe to share with students

---

# 7. Class Dashboard

Each class must have a dedicated detail page.

Example:

```text
Grade 10 - Einstein

Adviser:
John Doe

School Year:
2026–2027

Class Code:
A7K2P9
```

The Class Detail page contains:

```text
Students
Subjects
```

---

# 8. Class Students Tab

Display an interactive student table.

Required fields:

```text
LRN
Full Name
Email
Enrollment Status
```

Possible enrollment states:

```text
PENDING
APPROVED
DECLINED
REMOVED
```

The Adviser can:

- View students
- Approve join requests
- Decline join requests
- Remove students from the class where permitted

---

# 9. Student Class Joining

Students join classes using a Class Code.

Student workflow:

```text
Student Dashboard
       ↓
Join Class
       ↓
Enter 6-character Class Code
       ↓
Backend validates Class Code
       ↓
Enrollment Request created
       ↓
Class Adviser reviews request
       ↓
Approve / Decline
```

A student must not gain access to the class while the request is pending.

Once approved:

```text
Student
   ↓
Class Section
   ↓
All Subjects linked to Class
```

The student automatically gains access to every Subject currently linked to that Class Section.

If a new Subject is linked to the class later, enrolled students automatically gain access to it.

---

# 10. Subject Management — Subject Teacher

A Teacher can create multiple Subjects.

## 10.1 Subject Creation

Required field:

```text
Subject Name
```

Example:

```text
Mathematics
```

The backend generates a unique:

```text
7-character alphanumeric Subject Code
```

Example:

```text
MATH7K2
```

Subject Codes must:

- Be generated server-side
- Be unique
- Be shareable with Class Advisers
- Not expose database IDs

---

# 11. Subject Dashboard

A Subject contains:

```text
Classes
Requests
```

The Subject Teacher can see all Class Sections currently linked to the Subject.

If no class is linked:

```text
No classes joined yet.
```

Selecting a linked Class opens the Subject Management area.

---

# 12. Linking a Subject to a Class

The Class Adviser initiates the connection.

Workflow:

```text
Class Adviser
     ↓
Open Class
     ↓
Subjects
     ↓
Join Subject
     ↓
Enter 7-character Subject Code
     ↓
Backend validates code
     ↓
Subject Join Request
     ↓
Subject Teacher receives request
     ↓
Approve / Decline
```

The Subject must not become available to the Class until the Subject Teacher approves the request.

After approval:

```text
Class
   ↓
Linked Subject
   ↓
All enrolled students automatically receive Subject access
```

---

# 13. Subject Requests Tab

Subject Teachers must have a Requests area.

Each request displays:

```text
Section Name
Grade Level
School Year
Requesting Teacher / Adviser
Request Date
Status
```

Available actions:

```text
Approve
Decline
```

Once approved, the Class Section becomes linked to the Subject.

A duplicate Class → Subject relationship must not be allowed.

---

# 14. Subject Management Sub-Menu

Once a Class is selected inside a Subject, display:

```text
Materials
Requirements
Progress
Student Record
```

These modules operate within the selected:

```text
Class + Subject
```

context.

---

# 15. Materials

Subject Teachers can upload learning resources.

Supported examples:

- PPT / PPTX
- PDF
- DOC / DOCX
- Other explicitly supported educational file types

Required metadata:

```text
Title
Description
File Name
Content Type
Storage Key
File Size
Uploaded By
Created At
```

Materials belong to a specific:

```text
Class + Subject
```

Students have read-only access.

Students can:

- View
- Preview where supported
- Download

Do not expose unrestricted filesystem paths.

---

# 16. Requirements / Activities

Requirements are categorized into exactly three categories:

1. Written Activity
2. Performance Task
3. Test

Use these labels consistently throughout the UI and backend.

Do not silently introduce additional academic categories.

---

# 17. Activity Creation

Required fields:

```text
Activity Type
Title
Perfect Score
Deadline
Instructions
Allow Student Self-Submission Score
```

Optional:

```text
Attachment
```

Example:

```text
Activity Type:
Written Activity

Title:
Activity 5 - Programming

Perfect Score:
20

Deadline:
August 15, 2026 11:59 PM

Allow Student Self-Submission Score:
Enabled
```

Activities belong to a specific:

```text
Class + Subject
```

---

# 18. Activity View

The Teacher Activity View must display a student submission list.

For each student show:

```text
Student Name
Submission Status
Submission Date
Attached Files
Score
Maximum Score
```

Possible submission states:

```text
NOT_SUBMITTED
SUBMITTED
LATE
GRADED
```

Teachers can:

- View submissions
- Download attachments
- Enter scores
- Modify scores
- Review student score submissions

---

# 19. Student Requirement View

Students can see:

```text
Activity Title
Activity Type
Instructions
Perfect Score
Deadline
Submission Status
Current Score
```

Students can upload their required work.

The upload must use the native resumable object-storage pipeline described in Section 27.

---

# 20. Student Self-Submitted Score

Teachers can enable:

```text
Allow Student Self-Submission Score
```

When enabled, students may submit:

```text
Reported Score
Score-Proof Photograph
```

The photograph may be:

- Selected from the device
- Captured through the browser camera where supported

The score submission must enter a review workflow.

---

# 21. Score Submission State Machine

Use:

```text
PENDING
   |
   +----> APPROVED
   |
   +----> REJECTED
   |
   +----> EDITED / APPROVED
```

Rules:

- PENDING scores are not included in grade calculation.
- REJECTED scores are not included in grade calculation.
- APPROVED scores are included.
- Teacher-edited approved scores are included.
- The backend is authoritative.

A student must never directly modify an already approved grade.

---

# 22. Teacher Gradebook

Each Subject/Class combination has a Student Record / Gradebook.

The gradebook must display:

```text
Student
LRN
Written Activities
Performance Tasks
Tests
Category Averages
Final Grade
```

Individual activity scores must remain visible.

---

# 23. Grade Configuration

Teachers configure:

```text
Written Activity %
Performance Task %
Test %
```

Example:

```text
Written Activities: 40%
Performance Tasks: 40%
Tests: 20%
```

The backend must validate:

```text
Written Weight
+ Performance Weight
+ Test Weight
= 100%
```

Do not allow invalid grading configurations to be used for final-grade calculation.

---

# 24. Grade Calculation

The backend calculates category averages and final grades.

General formula:

```text
Final Grade =
    (Written Average × Written Weight)
  + (Performance Average × Performance Weight)
  + (Test Average × Test Weight)
```

Category averages must be calculated from valid graded activities according to the implemented grading policy.

Do not calculate final grades in a way that allows the frontend to manipulate the result.

The backend must remain authoritative.

Whenever a score changes:

```text
Score Changed
     ↓
Recalculate affected category
     ↓
Recalculate final grade
     ↓
Update progress/grade views
```

---

# 25. Excel Gradebook Export

Teachers must be able to export the current Class + Subject gradebook as:

```text
.xlsx
```

The generated spreadsheet should contain:

```text
Student Name
LRN
Individual Activity Scores
Written Activity Average
Performance Task Average
Test Average
Final Grade
```

The export must preserve the configured grading weights.

Use a suitable Java Excel library such as Apache POI.

The exported file must be generated server-side.

---

# 26. Student Progress

The Progress module provides real-time or immediately refreshed completion tracking.

For every student display:

```text
Student
Completion Percentage
Missing Activities
Current Grade
Status
```

Example:

```text
Student     Completion    Missing    Grade    Status
-----------------------------------------------------
Rhiane      100%          0          94       On Track
Jonelyn     70%           3          82       Needs Attention
Chuasan     45%           6          75       At Risk
```

The exact thresholds for:

```text
On Track
Needs Attention
At Risk
```

must be explicitly configured during implementation.

Do not silently invent thresholds.

---

# 27. Resumable Chunked File Upload Architecture

The system must implement a resilient file upload pipeline using:

```text
React
    ↓
Spring Boot
    ↓
MinIO S3-Compatible Object Storage
```

The PostgreSQL database stores file metadata and references, not large binary files.

The upload architecture must use **S3 Multipart Upload**.

---

## 27.1 Chunk Size

Default chunk size:

```text
5 MB
```

Frontend must use:

```javascript
Blob.slice()
```

to divide the file into chunks.

For a file of size `N`:

```text
numberOfParts =
    Math.ceil(file.size / 5MB)
```

---

# 28. Upload Initialization

Frontend sends:

```http
POST /api/uploads/initialize
```

Request:

```json
{
  "fileName": "activity-answer.pdf",
  "totalSizeBytes": 10485760,
  "contentType": "application/pdf"
}
```

Backend:

1. Authenticates the user.
2. Validates the requested upload.
3. Generates a secure object key.
4. Initiates an S3/MinIO multipart upload.
5. Receives the MinIO Upload ID.
6. Generates presigned PUT URLs for each part.
7. Returns upload metadata.

Response should contain conceptually:

```json
{
  "uploadId": "...",
  "fileKey": "...",
  "chunkSize": 5242880,
  "totalParts": 2,
  "presignedUrls": [
    {
      "partNumber": 1,
      "url": "..."
    },
    {
      "partNumber": 2,
      "url": "..."
    }
  ]
}
```

Do not expose MinIO credentials to the browser.

---

# 29. Direct Frontend-to-MinIO Upload

The frontend uploads each chunk directly to MinIO.

Flow:

```text
Browser
   |
   | PUT chunk
   ↓
MinIO
```

The Spring Boot backend must not proxy every file byte unless specifically required.

For every uploaded part, capture:

```text
partNumber
ETag
```

The browser must retain the ETag values.

---

# 30. Retry Logic

Each chunk must support automatic retry.

Default:

```text
Maximum attempts: 3
```

Example:

```text
Upload Part 4
   ↓
Failure
   ↓
Retry
   ↓
Failure
   ↓
Retry
   ↓
Failure
   ↓
Mark upload failed
```

Retries should handle transient HTTP/network failures.

Do not endlessly retry permanent errors such as invalid authorization.

---

# 31. Upload Progress

The frontend upload utility must expose progress information.

At minimum:

```text
uploadedBytes
totalBytes
percentage
currentPart
totalParts
```

The UI should display a progress indicator.

Example:

```text
Uploading...
██████████████░░░░░░ 72%
```

---

# 32. Multipart Completion

After every part has successfully uploaded:

```http
POST /api/uploads/complete
```

Request:

```json
{
  "uploadId": "...",
  "fileKey": "...",
  "parts": [
    {
      "partNumber": 1,
      "eTag": "..."
    },
    {
      "partNumber": 2,
      "eTag": "..."
    }
  ]
}
```

The backend must:

1. Authenticate the user.
2. Validate ownership of the upload.
3. Validate the part list.
4. Complete the MinIO multipart upload.
5. Persist the resulting file metadata.
6. Associate the file with the appropriate LMS entity.
7. Return the resulting file/submission identifier.

---

# 33. Upload Security

The backend must validate:

- File size
- Content type
- File extension where appropriate
- User authorization
- Target Class/Subject/Activity ownership
- Upload ownership
- Multipart upload ownership

Never trust:

```text
fileName
contentType
fileKey
uploadId
```

from the frontend without backend validation.

Storage keys must be generated server-side.

Do not allow users to select arbitrary object-storage paths.

---

# 34. Upload Lifecycle

Use a persistent upload record where appropriate.

Conceptual states:

```text
INITIALIZED
UPLOADING
COMPLETING
COMPLETED
FAILED
ABORTED
```

Abandoned multipart uploads should eventually be cleaned up.

The implementation may include a scheduled cleanup process for stale incomplete uploads.

Do not add complex infrastructure unless it is necessary.

---

# 35. Required Upload Backend APIs

At minimum:

```http
POST /api/uploads/initialize
POST /api/uploads/complete
```

Recommended additional endpoints where necessary:

```http
POST /api/uploads/{uploadId}/abort
GET  /api/uploads/{uploadId}
```

The exact API may be refined during implementation.

---

# 36. Required TypeScript Upload Utility

Implement:

```typescript
uploadAssignment(file)
```

The utility must:

- Automatically divide files into 5 MB chunks
- Request upload initialization
- Upload chunks directly to MinIO
- Capture ETags
- Retry failed chunks up to 3 times
- Track progress
- Complete the multipart upload
- Return a typed result
- Throw meaningful errors

Use strong TypeScript types.

Avoid:

```typescript
any
```

unless technically unavoidable.

The upload utility should support progress callbacks such as:

```typescript
onProgress(progress)
```

---

# 37. Docker Compose Infrastructure

Provide a development-ready Docker Compose configuration containing:

```text
PostgreSQL
MinIO
```

MinIO must provide:

- S3-compatible API
- Persistent storage volume
- Console access
- Development credentials through environment variables

Create/use the default bucket:

```text
lms-assignments
```

The architecture must support CORS for the frontend development origin:

```text
http://localhost:3000
```

including:

```text
PUT
```

and other required methods.

Do not hardcode production secrets.

Use `.env` variables for credentials.

---

# 38. Technology Stack

## Frontend

Use:

- React
- TypeScript
- HTML5
- Tailwind CSS
- React Router where appropriate
- Component-based architecture

The frontend communicates with Spring Boot through REST APIs.

---

## Backend

Use:

- Java 21+
- Spring Boot 3+
- Spring Web
- Spring Security
- Spring Data JPA
- Hibernate
- Bean Validation
- REST APIs
- Apache POI for `.xlsx` export
- WebSocket only where genuinely useful

Use Maven 3.9+.

---

## Database

Primary:

```text
PostgreSQL
```

Use:

```text
JPA / Hibernate
```

Do not place raw database access throughout controllers.

---

## Storage

Development:

```text
MinIO
```

Architecture should remain compatible with:

```text
Amazon S3
Other S3-compatible storage
```

---

# 39. Suggested Database Model

Use a normalized relational design.

Core entities should include at minimum:

```text
users
teachers
students

classes / sections
class_enrollments
class_join_requests

subjects
class_subjects
subject_join_requests

materials

activities
activity_submissions

scores
score_submissions

grading_configurations

announcements
notifications

file_uploads
```

Additional entities may be introduced when justified by the relationships.

---

# 40. Important Relationships

Conceptually:

```text
Teacher
 ├── Class Sections as Adviser
 │     ├── Student Enrollments
 │     └── Class Subjects
 │
 └── Subjects as Subject Teacher
       ├── Class Subjects
       ├── Materials
       ├── Activities
       ├── Scores
       └── Grade Configuration


Student
 ├── Class Enrollment
 │     └── Class Section
 │           └── Linked Subjects
 │
 ├── Activity Submissions
 ├── Scores
 ├── Score Submissions
 └── Notifications
```

A student should not need a separate manual enrollment record for every subject if subject access can be derived from:

```text
Student → Class → ClassSubject
```

Implement explicit subject enrollment records only if the actual business rules require them.

---

# 41. Backend Architecture

Use a modular monolith.

Suggested structure:

```text
backend/
└── src/main/java/com/apptitle/
    ├── auth/
    ├── user/
    ├── teacher/
    ├── student/
    ├── section/
    ├── enrollment/
    ├── subject/
    ├── material/
    ├── activity/
    ├── submission/
    ├── score/
    ├── grade/
    ├── progress/
    ├── announcement/
    ├── notification/
    ├── file/
    ├── export/
    ├── config/
    └── common/
```

Within each feature:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Use DTOs between API boundaries where appropriate.

Do not place business logic directly inside controllers.

---

# 42. Frontend Architecture

Suggested structure:

```text
frontend/
└── src/
    ├── components/
    ├── layouts/
    ├── pages/
    │   ├── auth/
    │   ├── teacher/
    │   │   ├── dashboard/
    │   │   ├── classes/
    │   │   └── subjects/
    │   └── student/
    ├── services/
    ├── hooks/
    ├── types/
    ├── utils/
    ├── upload/
    └── routes/
```

Use reusable components.

Avoid duplicating UI logic between Teacher and Student pages.

---

# 43. Teacher Navigation

The primary Teacher navigation should be:

```text
Dashboard
Classes
Subjects
Notifications
Profile
```

Class Adviser workflows live under:

```text
Classes
```

Subject Teacher workflows live under:

```text
Subjects
```

---

# 44. Student Navigation

Student navigation should include:

```text
Dashboard
My Class
Subjects
Progress
Grades
Notifications
Profile
```

The student should not need to manually select individual subjects to gain access.

---

# 45. Recommended Routes

Authentication:

```text
/auth/login
/auth/register
```

Teacher:

```text
/teacher/dashboard
/teacher/classes
/teacher/classes/:id
/teacher/subjects
/teacher/subjects/:id
/teacher/subjects/:id/classes/:classId
/teacher/notifications
```

Student:

```text
/student/dashboard
/student/class
/student/subjects
/student/subjects/:id
/student/progress
/student/grades
/student/notifications
```

The exact route structure may be refined during implementation.

---

# 46. API Design

Use RESTful APIs.

Authentication:

```http
POST /api/auth/register/teacher
POST /api/auth/register/student
POST /api/auth/login
```

Classes:

```http
GET  /api/teacher/classes
POST /api/teacher/classes

GET  /api/classes/{id}
GET  /api/classes/{id}/students
GET  /api/classes/{id}/subjects
```

Class enrollment:

```http
POST /api/classes/join
GET  /api/classes/{id}/join-requests
PUT  /api/class-enrollments/{id}/approve
PUT  /api/class-enrollments/{id}/decline
```

Subjects:

```http
GET  /api/teacher/subjects
POST /api/teacher/subjects
GET  /api/subjects/{id}
```

Subject linking:

```http
POST /api/classes/{classId}/subjects/join
GET  /api/subjects/{subjectId}/requests
PUT  /api/subject-join-requests/{id}/approve
PUT  /api/subject-join-requests/{id}/decline
```

Materials:

```http
GET  /api/classes/{classId}/subjects/{subjectId}/materials
POST /api/classes/{classId}/subjects/{subjectId}/materials
```

Activities:

```http
GET  /api/classes/{classId}/subjects/{subjectId}/activities
POST /api/classes/{classId}/subjects/{subjectId}/activities
GET  /api/activities/{id}
```

Submissions:

```http
POST /api/activities/{id}/submissions
GET  /api/activities/{id}/submissions
```

Scores:

```http
GET /api/activities/{id}/scores
PUT /api/activities/{id}/scores
```

Score submissions:

```http
POST /api/activities/{id}/score-submissions
PUT /api/score-submissions/{id}/approve
PUT /api/score-submissions/{id}/reject
PUT /api/score-submissions/{id}/edit
```

Grades:

```http
GET /api/classes/{classId}/subjects/{subjectId}/grades
GET /api/students/me/grades
```

Progress:

```http
GET /api/classes/{classId}/subjects/{subjectId}/progress
GET /api/students/me/progress
```

Excel:

```http
GET /api/classes/{classId}/subjects/{subjectId}/grades/export
```

Uploads:

```http
POST /api/uploads/initialize
POST /api/uploads/complete
POST /api/uploads/{uploadId}/abort
```

Notifications:

```http
GET /api/notifications
PUT /api/notifications/{id}/read
```

---

# 47. Authorization Rules

Backend authorization is mandatory.

## Teacher

A Teacher can:

- Manage only their own classes where they are Adviser.
- Manage only their own Subjects.
- Approve students joining their classes.
- Request subjects to be linked to their classes.
- Approve Subject join requests only for Subjects they own.
- Manage materials for authorized Class + Subject combinations.
- Create activities for authorized Class + Subject combinations.
- Record scores for their activities.
- Approve score submissions for their activities.
- Export gradebooks for authorized Class + Subject combinations.

## Student

A Student can:

- Access only their approved Class Section.
- Access only Subjects linked to that Class.
- View only authorized materials.
- Submit only available requirements.
- View only their own grades.
- View only their own progress.
- Submit self-scores only when enabled.
- Never directly modify approved grades.

Never rely only on frontend route protection or hidden buttons.

---

# 48. Security Requirements

At minimum:

- Secure password hashing
- Spring Security
- Role-based authorization
- Backend ownership checks
- Backend input validation
- Bean Validation
- File type validation
- File size validation
- Secure object keys
- No exposed storage credentials
- No hardcoded secrets
- Environment variables for credentials
- HTTPS in production
- Protection against unauthorized resource access
- Protection against arbitrary file paths
- Protection against manipulating IDs belonging to other users
- Backend-authoritative grade calculation
- Backend-authoritative enrollment approval

Never store plaintext passwords.

Never allow a student to submit an arbitrary grade directly to the gradebook.

---

# 49. Responsive Design

The UI must be responsive.

Desktop layouts may use:

```text
Sidebar
Top navigation
Tables
Multi-column dashboards
```

Mobile layouts should adapt to:

```text
Collapsible navigation
Cards
Scrollable tables
Mobile-friendly forms
Large touch targets
```

The following must work well on mobile:

- Joining classes
- Viewing activities
- Uploading assignments
- Taking/uploading score-proof photographs
- Viewing grades
- Viewing progress
- Reading announcements
- Notifications

---

# 50. Notifications

Persistent notifications must be stored in PostgreSQL.

Students should receive notifications for:

- New announcements
- New learning materials
- New activities
- Upcoming deadlines
- Score submission requests
- Released grades

Teachers should receive notifications for:

- Student join requests
- Subject join requests
- Student submissions
- Student score submissions

WebSocket real-time delivery is optional.

The database notification record is the authoritative source.

---

# 51. Development Phases

Do not attempt to build everything simultaneously.

## Phase 1 — Foundation

- Spring Boot project
- React project
- PostgreSQL
- MinIO
- Docker Compose
- Environment configuration
- Frontend/backend communication

## Phase 2 — Authentication

- Teacher registration
- Student registration
- Login
- Spring Security
- Password hashing
- Role authorization

## Phase 3 — Class Management

- Teacher class creation
- 6-character Class Code
- Class dashboard
- Student join request
- Adviser approval/decline
- Student roster

## Phase 4 — Subject Management

- Subject creation
- 7-character Subject Code
- Subject dashboard
- Class → Subject join request
- Subject Teacher approval
- Automatic student subject access

## Phase 5 — Materials

- Material creation
- MinIO integration
- Resumable multipart uploads
- Student material access

## Phase 6 — Activities

- Three activity categories
- Activity creation
- Deadlines
- Instructions
- Attachments
- Student submissions

## Phase 7 — Grading

- Teacher score entry
- Score validation
- Student self-score submission
- Proof image upload
- Approval/rejection
- Grade calculation
- Grade configuration

## Phase 8 — Progress

- Completion calculation
- Missing activities
- Current grades
- Student status
- Teacher progress dashboard

## Phase 9 — Gradebook Export

- Full gradebook
- Itemized scores
- Category averages
- Final grades
- `.xlsx` export

## Phase 10 — Announcements and Notifications

- Announcements
- Persistent notifications
- Deadline notifications
- Submission notifications
- Grade notifications
- Optional WebSocket delivery

## Phase 11 — Production Readiness

- Security review
- Validation
- Error handling
- Automated tests
- Responsive UI refinement
- Upload security review
- Database backup strategy
- Deployment configuration
- Documentation

---

# 52. Testing Requirements

Add automated tests for critical business logic.

At minimum test:

### Authentication

- Teacher registration
- Student registration
- Duplicate email rejection
- Duplicate LRN rejection
- Password hashing
- Role authorization

### Class Enrollment

- Valid Class Code
- Invalid Class Code
- Duplicate enrollment request
- Adviser approval
- Adviser rejection
- Unauthorized approval attempts

### Subject Linking

- Valid Subject Code
- Invalid Subject Code
- Duplicate Class → Subject relationship
- Subject Teacher approval
- Subject Teacher rejection
- Unauthorized approval attempts

### Grades

- Correct category averages
- Correct weighted final grade
- Invalid weight totals
- Pending score exclusion
- Rejected score exclusion
- Approved score inclusion
- Teacher-edited score handling

### Uploads

- Upload initialization
- Invalid file type
- Invalid file size
- Unauthorized upload
- Multipart completion
- Failed part retry handling

---

# 53. Data Integrity Rules

The backend must enforce:

```text
Class Code → Unique
Subject Code → Unique
Teacher Email → Unique
Student Email → Unique
Student LRN → Unique
Class Enrollment → No duplicate approved membership
Class + Subject → No duplicate link
```

Use database constraints in addition to service-layer validation wherever practical.

Do not rely solely on frontend validation.

---

# 54. System Data Flow

## Teacher Creating a Class

```text
Teacher Login
     ↓
Teacher Dashboard
     ↓
Create Class
     ↓
Enter Grade Level + Section + School Year
     ↓
Spring Boot validates request
     ↓
Database creates Class
     ↓
Backend generates 6-character Class Code
     ↓
Class Dashboard
```

## Student Joining a Class

```text
Student Login
     ↓
Join Class
     ↓
Enter Class Code
     ↓
Backend validates Class Code
     ↓
Create Join Request
     ↓
Adviser Notification
     ↓
Approve / Decline
     ↓
Approved Enrollment
     ↓
Student enters Class
```

## Adviser Linking Subject

```text
Adviser opens Class
     ↓
Subjects
     ↓
Join Subject
     ↓
Enter 7-character Subject Code
     ↓
Backend validates Subject
     ↓
Create Subject Join Request
     ↓
Subject Teacher Notification
     ↓
Approve
     ↓
Class ↔ Subject relationship created
     ↓
All enrolled students gain Subject access
```

## Student Assignment Submission

```text
Student opens Subject
     ↓
Requirements
     ↓
Select Activity
     ↓
Select File
     ↓
Initialize Upload
     ↓
Spring Boot → MinIO Multipart Upload
     ↓
Receive Presigned URLs
     ↓
Browser uploads 5 MB chunks
     ↓
Capture ETags
     ↓
Retry failed chunks up to 3 times
     ↓
Complete Multipart Upload
     ↓
Persist File Metadata
     ↓
Create Submission
```

## Grade Calculation

```text
Student Score
     ↓
Backend Validation
     ↓
Approved Score
     ↓
Activity Score
     ↓
Category Average
     ↓
Configured Weight
     ↓
Final Grade
     ↓
Progress / Gradebook
     ↓
Student Dashboard
```

---

# 55. High-Level Architecture

```text
                         USERS
                    /             \
               TEACHER           STUDENT
                  |                 |
                  +--------+--------+
                           |
                    React + TypeScript
                           |
                     REST API / HTTPS
                           |
                    Spring Boot 3+
                           |
        +------------------+------------------+
        |                  |                  |
 Spring Security       Services          Upload Service
        |                  |                  |
        |          +-------+-------+          |
        |          |       |       |          |
        |        JPA    Grade   Progress     |
        |          |       |       |          |
        +----------+-------+-------+----------+
                           |
                       PostgreSQL
                           |
                    File Metadata
                           |
                        MinIO
                    Object Storage
```

The frontend must never communicate directly with PostgreSQL.

For file contents, the frontend may communicate directly with MinIO only through securely generated presigned URLs issued by the backend.

---

# 56. Expected End-to-End Teacher Workflow

```text
Teacher
 ↓
Register / Login
 ↓
Create Class Section
 ↓
Receive 6-character Class Code
 ↓
Share Class Code with Students
 ↓
Approve Student Join Requests
 ↓
Create Subjects
 ↓
Share 7-character Subject Codes with Advisers
 ↓
Approve Class Subject Requests
 ↓
Open Class + Subject
 ↓
Upload Materials
 ↓
Create Requirements
 ↓
Configure Grade Weights
 ↓
Review Submissions
 ↓
Record / Approve Scores
 ↓
Monitor Student Progress
 ↓
View Gradebook
 ↓
Export .xlsx
 ↓
Post Announcements / Review Notifications
```

---

# 57. Expected End-to-End Student Workflow

```text
Student
 ↓
Register
 ↓
Login
 ↓
Enter 6-character Class Code
 ↓
Wait for Adviser Approval
 ↓
Class Approved
 ↓
Automatically Receive Linked Subjects
 ↓
Open Subject
 ↓
View Materials
 ↓
View Requirements
 ↓
Submit Work
 ↓
Upload Files
 ↓
View Submission Status
 ↓
Submit Score + Proof When Enabled
 ↓
Teacher Reviews Score
 ↓
View Approved Score
 ↓
View Progress
 ↓
View Category Averages
 ↓
View Final Grade
 ↓
Receive Announcements / Notifications
```

---

# 58. Required Implementation Deliverables

The implementation must eventually provide:

## Backend

1. Complete Java 21 Spring Boot project.
2. Spring Security authentication.
3. Teacher/student authorization.
4. Class management.
5. Class enrollment workflow.
6. Subject management.
7. Subject linking workflow.
8. Materials management.
9. Activity management.
10. Student submissions.
11. Score management.
12. Student score approval workflow.
13. Backend grade calculation.
14. Progress calculation.
15. Announcements.
16. Notifications.
17. Excel gradebook export.
18. MinIO multipart upload service.
19. Upload initialization endpoint.
20. Upload completion endpoint.
21. Appropriate DTOs, entities, services, repositories, and validation.
22. Automated tests for critical business logic.

## Frontend

1. React + TypeScript application.
2. Responsive Teacher dashboard.
3. Responsive Student dashboard.
4. Authentication pages.
5. Class management interface.
6. Class enrollment interface.
7. Subject management interface.
8. Subject request interface.
9. Materials interface.
10. Requirements interface.
11. Submission interface.
12. Gradebook.
13. Progress dashboard.
14. Notifications.
15. Excel export action.
16. Fully typed 5 MB chunked upload utility.
17. Upload progress UI.
18. Retry handling.

## Infrastructure

1. Docker Compose.
2. PostgreSQL.
3. MinIO.
4. Persistent volumes.
5. Development environment variables.
6. `lms-assignments` bucket.
7. MinIO CORS configuration.
8. Setup documentation.

---

# 59. Production-Quality Expectations

The implementation must not be a mockup or static prototype.

Build an actual working application with:

- Real database persistence
- Real authentication
- Real authorization
- Real enrollment
- Real subject linking
- Real file storage
- Real submissions
- Real score records
- Real grade calculations
- Real progress calculations
- Real notifications
- Real Excel export

Avoid hardcoded sample data except for development/test fixtures.

Do not simulate backend behavior inside React.

---

# 60. Important Implementation Rules

When implementing this project:

1. Treat this document as the baseline specification.
2. Do not remove planned functionality without explicit instruction.
3. Do not invent major user-facing requirements.
4. When implementation details are unspecified, choose a sensible technical implementation while preserving the requested behavior.
5. Keep the backend authoritative.
6. Keep frontend and backend concerns separated.
7. Use DTOs for API boundaries where appropriate.
8. Validate all important input on the backend.
9. Keep database relationships normalized.
10. Keep the application runnable after every major development phase.
11. Write automated tests for critical business logic.
12. Document setup and environment variables.
13. Never hardcode credentials.
14. Never expose MinIO credentials to the frontend.
15. Do not store large uploaded files directly in PostgreSQL.
16. Use MinIO/S3-compatible object storage for file contents.
17. Use 5 MB multipart chunks for the requested upload implementation.
18. Implement retry logic for failed chunks.
19. Persist upload/file metadata.
20. Enforce upload ownership.
21. Keep grade computation server-side.
22. Keep score approval server-side.
23. Keep Class Code generation server-side.
24. Keep Subject Code generation server-side.
25. Prevent duplicate Class–Subject relationships.
26. Preserve exactly three activity categories:
    - Written Activity
    - Performance Task
    - Test
27. Do not introduce microservices.
28. Do not add Redis, Kubernetes, or other infrastructure unless there is a demonstrated requirement.
29. Prefer understandable code appropriate for a school project.
30. Build incrementally rather than attempting the entire application in one uncontrolled generation.

---

# 61. Success Criteria

## Teacher

- [ ] Teacher can register and log in.
- [ ] Teacher can create Class Sections.
- [ ] System generates unique 6-character Class Codes.
- [ ] Teacher can approve/decline student join requests.
- [ ] Teacher can create Subjects.
- [ ] System generates unique 7-character Subject Codes.
- [ ] Adviser can request a Subject for a Class.
- [ ] Subject Teacher can approve/decline Subject requests.
- [ ] Approved Subjects become available to all students in the Class.
- [ ] Teacher can upload materials.
- [ ] Teacher can create Written Activities.
- [ ] Teacher can create Performance Tasks.
- [ ] Teacher can create Tests.
- [ ] Teacher can configure grading percentages.
- [ ] Teacher can record scores.
- [ ] Teacher can review student score submissions.
- [ ] Teacher can approve/reject/edit score submissions.
- [ ] Teacher can monitor progress.
- [ ] Teacher can view category averages.
- [ ] Teacher can view final grades.
- [ ] Teacher can export `.xlsx` gradebooks.
- [ ] Teacher can post announcements.
- [ ] Teacher receives relevant notifications.

## Student

- [ ] Student can register.
- [ ] Student can log in.
- [ ] Student can join a Class using a 6-character Class Code.
- [ ] Student remains pending until Adviser approval.
- [ ] Student automatically receives linked Subjects after Class approval.
- [ ] Student can view Class information.
- [ ] Student can view linked Subjects.
- [ ] Student can view learning materials.
- [ ] Student can submit requirements.
- [ ] Student can upload files through the resumable MinIO pipeline.
- [ ] Student can retry failed uploads.
- [ ] Student can see upload progress.
- [ ] Student can submit a score when enabled.
- [ ] Student can upload score-proof photographs.
- [ ] Student can see submission status.
- [ ] Student can view scores.
- [ ] Student can view category averages.
- [ ] Student can view final grades.
- [ ] Student can view progress.
- [ ] Student can see missing requirements.
- [ ] Student can receive notifications.

## Technical

- [ ] React + TypeScript frontend works with Spring Boot backend.
- [ ] Java 21+ is used.
- [ ] Spring Boot 3+ is used.
- [ ] Spring Security protects the application.
- [ ] PostgreSQL persistence works.
- [ ] MinIO object storage works.
- [ ] Docker Compose starts required development infrastructure.
- [ ] File uploads use 5 MB multipart chunks.
- [ ] Failed chunks retry up to three times.
- [ ] ETags are captured and submitted correctly.
- [ ] Multipart uploads complete successfully.
- [ ] Upload metadata is persisted.
- [ ] Backend enforces authorization.
- [ ] Backend calculates grades.
- [ ] Backend calculates progress.
- [ ] `.xlsx` gradebook export works.
- [ ] Critical business logic has automated tests.
- [ ] UI is responsive.
- [ ] Setup and run instructions are documented.

---

# 62. Final Architecture Summary

```text
FRONTEND
React
TypeScript
Tailwind CSS
React Router

        ↓ REST / HTTPS

BACKEND
Java 21+
Spring Boot 3+
Spring Web
Spring Security
Spring Data JPA
Hibernate
Bean Validation
Apache POI

        ↓

DATABASE
PostgreSQL

        ↓

OBJECT STORAGE
MinIO
S3-Compatible Multipart Upload

        ↓

INFRASTRUCTURE
Docker Compose

ARCHITECTURE
Modular Monolith
```

Core business hierarchy:

```text
Teacher
   │
   ├── Adviser
   │     └── Class
   │           ├── Students
   │           └── Subjects
   │
   └── Subject Teacher
         └── Subject
               ├── Materials
               ├── Requirements
               ├── Progress
               └── Gradebook

Student
   │
   └── Approved Class
         └── Linked Subjects
```

The goal is a **real, maintainable, responsive LMS web application**, not a static website, mockup, or desktop Java application.

The system should provide the simplicity of a classroom-management platform while adding the requested Class Adviser/Subject Teacher hierarchy, automatic subject access through class enrollment, automated grading, progress monitoring, Excel gradebook export, and resilient direct-to-MinIO file uploads.
