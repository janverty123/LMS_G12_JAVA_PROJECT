-- Learning Materials / Multipart Upload Migration
-- Run after class_section_subject_migration.sql and back up the database first.

BEGIN;

CREATE TABLE IF NOT EXISTS learning_materials (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    class_subject_link_id UUID NOT NULL,
    uploaded_by UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    storage_key VARCHAR(700) NOT NULL UNIQUE,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes > 0),
    upload_id VARCHAR(255) NOT NULL UNIQUE,
    chunk_size_bytes BIGINT NOT NULL CHECK (chunk_size_bytes > 0),
    total_parts INTEGER NOT NULL CHECK (total_parts > 0),
    status VARCHAR(20) NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_learning_material_class_subject_link
        FOREIGN KEY (class_subject_link_id)
        REFERENCES class_subject_links(id),

    CONSTRAINT fk_learning_material_uploaded_by
        FOREIGN KEY (uploaded_by)
        REFERENCES teachers(id),

    CONSTRAINT chk_learning_material_status
        CHECK (status IN ('INITIALIZED', 'COMPLETING', 'COMPLETED', 'FAILED', 'ABORTED'))
);

CREATE TABLE IF NOT EXISTS material_chunks (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    learning_material_id UUID NOT NULL,
    part_number INTEGER NOT NULL CHECK (part_number > 0),
    e_tag VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),

    CONSTRAINT fk_material_chunk_learning_material
        FOREIGN KEY (learning_material_id)
        REFERENCES learning_materials(id)
        ON DELETE CASCADE,

    CONSTRAINT unique_material_chunk_part
        UNIQUE (learning_material_id, part_number)
);

CREATE INDEX IF NOT EXISTS idx_learning_materials_link_status
    ON learning_materials(class_subject_link_id, status);

COMMIT;
