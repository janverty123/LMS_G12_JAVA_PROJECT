-- Activities migration. Run after class_section_subject_migration.sql.
BEGIN;

CREATE TABLE IF NOT EXISTS activities (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    class_subject_link_id UUID NOT NULL,
    created_by UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(150) NOT NULL,
    perfect_score NUMERIC(10, 2) NOT NULL CHECK (perfect_score > 0),
    deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    instructions VARCHAR(5000) NOT NULL,
    allow_student_self_submission_score BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_activity_class_subject_link FOREIGN KEY (class_subject_link_id)
        REFERENCES class_subject_links(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_created_by FOREIGN KEY (created_by)
        REFERENCES teachers(id),
    CONSTRAINT chk_activity_type CHECK (
        type IN ('WRITTEN_ACTIVITY', 'PERFORMANCE_TASK', 'TEST')
    )
);

CREATE INDEX IF NOT EXISTS idx_activities_link_deadline
    ON activities(class_subject_link_id, deadline);

CREATE TABLE IF NOT EXISTS activity_files (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    activity_id UUID NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    student_id UUID REFERENCES students(id),
    purpose VARCHAR(30) NOT NULL CHECK (
        purpose IN ('TEACHER_ATTACHMENT', 'STUDENT_SUBMISSION', 'SCORE_PROOF')
    ),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    storage_key VARCHAR(700) NOT NULL UNIQUE,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes > 0),
    upload_id VARCHAR(255) NOT NULL UNIQUE,
    chunk_size_bytes BIGINT NOT NULL CHECK (chunk_size_bytes > 0),
    total_parts INTEGER NOT NULL CHECK (total_parts > 0),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS activity_file_chunks (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    activity_file_id UUID NOT NULL REFERENCES activity_files(id) ON DELETE CASCADE,
    part_number INTEGER NOT NULL CHECK (part_number > 0),
    e_tag VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    UNIQUE (activity_file_id, part_number)
);

CREATE TABLE IF NOT EXISTS activity_submissions (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    activity_id UUID NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id),
    activity_file_id UUID REFERENCES activity_files(id),
    status VARCHAR(20) NOT NULL CHECK (
        status IN ('SUBMITTED', 'LATE', 'GRADED')
    ),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    score NUMERIC(10, 2),
    UNIQUE (activity_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_activity_submissions_activity
    ON activity_submissions(activity_id);

CREATE TABLE IF NOT EXISTS score_proposals (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    activity_id UUID NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id),
    proof_file_id UUID NOT NULL REFERENCES activity_files(id),
    reported_score NUMERIC(10, 2) NOT NULL CHECK (reported_score >= 0),
    approved_score NUMERIC(10, 2),
    status VARCHAR(30) NOT NULL CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'EDITED_APPROVED')
    ),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by UUID REFERENCES teachers(id),
    UNIQUE (activity_id, student_id)
);

COMMIT;
