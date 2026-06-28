CREATE TABLE ticket_event_comments (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    comment VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE business_follows (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    business_id VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_business_follows_user_business UNIQUE (user_id, business_id)
);

CREATE INDEX idx_ticket_comments_event_created ON ticket_event_comments (event_id, created_at);
CREATE INDEX idx_business_follows_user_active ON business_follows (user_id, active);
CREATE INDEX idx_business_follows_business_active ON business_follows (business_id, active);
