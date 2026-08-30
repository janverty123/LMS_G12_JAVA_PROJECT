BEGIN;

CREATE TABLE IF NOT EXISTS announcements (
    id UUID PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    class_section_id UUID NOT NULL REFERENCES class_sections(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES teachers(id), title VARCHAR(150) NOT NULL,
    content VARCHAR(5000) NOT NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL, message VARCHAR(200) NOT NULL,
    reference_key VARCHAR(150) NOT NULL, read BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (recipient_id, type, reference_key)
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created
    ON notifications(recipient_id, created_at DESC);
COMMIT;
