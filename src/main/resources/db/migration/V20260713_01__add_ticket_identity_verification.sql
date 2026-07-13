ALTER TABLE user_tickets
    ADD COLUMN IF NOT EXISTS verification_photo_object_name VARCHAR(512),
    ADD COLUMN IF NOT EXISTS verification_photo_captured_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS transferred_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS transferred_from_user_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS ownership_version INTEGER NOT NULL DEFAULT 1;

UPDATE user_tickets
SET ownership_version = 1
WHERE ownership_version IS NULL OR ownership_version < 1;

CREATE INDEX IF NOT EXISTS idx_user_tickets_user_status
    ON user_tickets (user_id, status);
