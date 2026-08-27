-- ClassSection/Subject Migration Script
-- Back up the database before running this script.

BEGIN;

-- 1. Create new tables

CREATE TABLE IF NOT EXISTS class_sections (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    adviser_id UUID NOT NULL,
    grade_level VARCHAR(50) NOT NULL,
    section VARCHAR(50) NOT NULL,
    school_year VARCHAR(9) NOT NULL,
    class_code VARCHAR(6) NOT NULL UNIQUE CHECK (length(class_code) = 6),
    
    CONSTRAINT fk_class_section_adviser
        FOREIGN KEY (adviser_id)
        REFERENCES teachers(id)
);

CREATE TABLE IF NOT EXISTS subjects (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    subject_teacher_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    subject_code VARCHAR(7) NOT NULL UNIQUE CHECK (length(subject_code) = 7),
    
    CONSTRAINT fk_subject_teacher
        FOREIGN KEY (subject_teacher_id)
        REFERENCES teachers(id),

    CONSTRAINT unique_teacher_subject_name
        UNIQUE (subject_teacher_id, name)
);

CREATE TABLE IF NOT EXISTS class_subject_links (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    class_section_id UUID NOT NULL,
    subject_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_by UUID NOT NULL,
    
    CONSTRAINT fk_class_subject_link_class_section
        FOREIGN KEY (class_section_id)
        REFERENCES class_sections(id),
    
    CONSTRAINT fk_class_subject_link_subject
        FOREIGN KEY (subject_id)
        REFERENCES subjects(id),
    
    CONSTRAINT fk_class_subject_link_requested_by
        FOREIGN KEY (requested_by)
        REFERENCES teachers(id),
    
    CONSTRAINT unique_class_subject_link
        UNIQUE (class_section_id, subject_id)
);

CREATE TABLE IF NOT EXISTS class_enrollment_requests (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    student_id UUID NOT NULL,
    class_section_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    
    CONSTRAINT fk_class_enrollment_request_student
        FOREIGN KEY (student_id)
        REFERENCES students(id),
    
    CONSTRAINT fk_class_enrollment_request_class_section
        FOREIGN KEY (class_section_id)
        REFERENCES class_sections(id),
    
    CONSTRAINT unique_student_class_section
        UNIQUE (student_id, class_section_id)
);

-- 2. Migrate data from sections to class_sections
-- Extract grade level, section, and school year from the old section name
-- Assuming the format was "Grade 12 - A (2026-2027)"
INSERT INTO class_sections (
    id, created_at, updated_at, adviser_id, 
    grade_level, section, school_year, class_code
)
SELECT 
    id, created_at, updated_at, teacher_id,
    -- Extract grade level (e.g., "Grade 12")
    CASE
        WHEN name LIKE 'Grade %' THEN
            CASE
                WHEN position(' -' IN name) > 0 THEN left(name, position(' -' IN name) - 1)
                ELSE name
            END
        ELSE 'Unknown'
    END AS grade_level,
    -- Extract section (e.g., "A")
    CASE
        WHEN name LIKE '% - %' THEN
            CASE
                WHEN position(' (' IN name) > 0 THEN
                    substring(name FROM position(' - ' IN name) + 3 FOR position(' (' IN name) - position(' - ' IN name) - 3)
                ELSE
                    substring(name FROM position(' - ' IN name) + 3)
            END
        ELSE 'Unknown'
    END AS section,
    -- Extract school year (e.g., "2026-2027")
    CASE
        WHEN name LIKE '%(%' THEN
            substring(name FROM position('(' IN name) + 1 FOR position(')' IN name) - position('(' IN name) - 1)
        ELSE '2023-2024' -- Default if not found
    END AS school_year,
    CASE
        WHEN length(trim(class_code)) = 6 THEN upper(trim(class_code))
        ELSE upper(substring(md5(id::text) FROM 1 FOR 6))
    END
FROM sections
ON CONFLICT (id) DO NOTHING;

-- 3. Create one Subject for each distinct teacher/name pair.
INSERT INTO subjects (id, created_at, updated_at, subject_teacher_id, name, subject_code)
SELECT 
    gen_random_uuid(),
    now(),
    now(),
    source.teacher_id,
    source.subject_name,
    upper(substring(md5(source.teacher_id::text || ':' || source.subject_name) FROM 1 FOR 7))
FROM (
    SELECT DISTINCT teacher_id, trim(subject_name) AS subject_name
    FROM sections
    WHERE subject_name IS NOT NULL AND trim(subject_name) <> ''
) source
ON CONFLICT (subject_teacher_id, name) DO NOTHING;

-- 4. Create class_subject_links for existing sections and their subjects
-- This assumes each section had one subject, which matches the old model
INSERT INTO class_subject_links (
    id, created_at, updated_at, class_section_id, subject_id, status, requested_by
)
SELECT 
    gen_random_uuid(),
    now(),
    now(),
    cs.id,
    sub.id,
    'APPROVED',
    cs.adviser_id
FROM class_sections cs
JOIN sections old ON cs.id = old.id
JOIN subjects sub
  ON sub.name = trim(old.subject_name)
 AND sub.subject_teacher_id = old.teacher_id
ON CONFLICT (class_section_id, subject_id) DO NOTHING;

-- 5. Preserve all existing student enrollment requests.
INSERT INTO class_enrollment_requests (
    id, created_at, updated_at, student_id, class_section_id, status
)
SELECT id, created_at, updated_at, student_id, section_id, status
FROM join_requests
ON CONFLICT (student_id, class_section_id) DO NOTHING;

-- The SRS permits only one approved Class Section per student. Abort instead
-- of silently changing data if the legacy database violates that rule.
DO $$
BEGIN
    IF EXISTS (
        SELECT student_id
        FROM class_enrollment_requests
        WHERE status = 'APPROVED'
        GROUP BY student_id
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Migration stopped: at least one student has multiple approved class sections.';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS unique_approved_class_per_student
    ON class_enrollment_requests (student_id)
    WHERE status = 'APPROVED';

COMMIT;

-- Legacy tables are intentionally retained for rollback inspection. After a
-- verified backup and successful application deployment, remove them manually:
-- DROP TABLE join_requests;
-- DROP TABLE sections;
