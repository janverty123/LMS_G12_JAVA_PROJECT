-- ClassSection/Subject Migration Script
-- Run this script in your PostgreSQL client to migrate data

-- 1. Create new tables

CREATE TABLE IF NOT EXISTS class_sections (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    adviser_id UUID NOT NULL,
    grade_level VARCHAR(50) NOT NULL,
    section VARCHAR(50) NOT NULL,
    school_year VARCHAR(9) NOT NULL,
    class_code VARCHAR(6) NOT NULL UNIQUE,
    
    CONSTRAINT fk_class_section_adviser
        FOREIGN KEY (adviser_id)
        REFERENCES teachers(id)
);

CREATE TABLE IF NOT EXISTS subjects (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    subject_teacher_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL UNIQUE,
    subject_code VARCHAR(7) NOT NULL UNIQUE,
    
    CONSTRAINT fk_subject_teacher
        FOREIGN KEY (subject_teacher_id)
        REFERENCES teachers(id)
);

CREATE TABLE IF NOT EXISTS class_subject_links (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
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
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
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
    class_code
FROM sections
ON CONFLICT (id) DO NOTHING;

-- 3. Create initial subjects for each teacher
-- This creates a subject for each unique subjectName in the old sections table
INSERT INTO subjects (id, created_at, updated_at, subject_teacher_id, name, subject_code)
SELECT 
    gen_random_uuid(),
    now(),
    now(),
    s.teacher_id,
    s.subject_name,
    -- Generate a unique subject code for each subject
    left(upper(replace(s.subject_name, ' ', '')), 4) || 
    lpad(floor(random() * 1000)::text, 3, '0')
FROM sections s
WHERE s.subject_name IS NOT NULL
ON CONFLICT (name) DO NOTHING;

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
JOIN subjects sub ON sub.name = old.subject_name AND sub.subject_teacher_id = old.teacher_id
ON CONFLICT (class_section_id, subject_id) DO NOTHING;

-- Note: After running this script, you'll need to:
-- 1. Update the join_requests table to reference class_sections instead of sections
-- 2. Rename join_requests to class_enrollment_requests
-- 3. Drop the old sections table

-- These final steps should be done after confirming the migration was successful:
-- ALTER TABLE join_requests RENAME TO class_enrollment_requests;
-- ALTER TABLE class_enrollment_requests RENAME COLUMN section_id TO class_section_id;
-- DROP TABLE sections;