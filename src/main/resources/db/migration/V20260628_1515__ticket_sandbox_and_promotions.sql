CREATE TABLE ticket_stripe_sessions (
    id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    event_id VARCHAR(64),
    user_id VARCHAR(64),
    stripe_session_id VARCHAR(180) NOT NULL UNIQUE,
    checkout_url VARCHAR(2048),
    payment_status VARCHAR(40) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    sandbox BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE TABLE ticket_event_promotions (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    owner_id VARCHAR(64) NOT NULL,
    stripe_session_id VARCHAR(180),
    amount NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(40) NOT NULL,
    starts_at TIMESTAMP,
    ends_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_ticket_stripe_sessions_payment ON ticket_stripe_sessions (payment_id);
CREATE INDEX idx_ticket_stripe_sessions_event_user ON ticket_stripe_sessions (event_id, user_id);
CREATE INDEX idx_ticket_event_promotions_event_status ON ticket_event_promotions (event_id, status);
CREATE INDEX idx_ticket_event_promotions_owner_status ON ticket_event_promotions (owner_id, status);
