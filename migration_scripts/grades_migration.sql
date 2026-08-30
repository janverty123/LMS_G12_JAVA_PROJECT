-- Grade configuration migration. Activity scores are stored on activity_submissions.
BEGIN;

CREATE TABLE IF NOT EXISTS grade_configurations (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    class_subject_link_id UUID NOT NULL UNIQUE
        REFERENCES class_subject_links(id) ON DELETE CASCADE,
    written_activity_weight NUMERIC(5, 2) NOT NULL CHECK (
        written_activity_weight BETWEEN 0 AND 100),
    performance_task_weight NUMERIC(5, 2) NOT NULL CHECK (
        performance_task_weight BETWEEN 0 AND 100),
    test_weight NUMERIC(5, 2) NOT NULL CHECK (test_weight BETWEEN 0 AND 100),
    CONSTRAINT chk_grade_weights_total CHECK (
        written_activity_weight + performance_task_weight + test_weight = 100)
);

COMMIT;
