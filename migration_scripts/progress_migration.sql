BEGIN;

CREATE TABLE IF NOT EXISTS progress_configurations (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    class_subject_link_id UUID NOT NULL UNIQUE
        REFERENCES class_subject_links(id) ON DELETE CASCADE,
    on_track_minimum NUMERIC(5, 2) NOT NULL CHECK (
        on_track_minimum BETWEEN 0 AND 100),
    needs_attention_minimum NUMERIC(5, 2) NOT NULL CHECK (
        needs_attention_minimum BETWEEN 0 AND 100),
    CONSTRAINT chk_progress_threshold_order CHECK (
        needs_attention_minimum < on_track_minimum)
);

COMMIT;
