CREATE TABLE ticket_events (
    id VARCHAR(64) PRIMARY KEY,
    owner_id VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    location VARCHAR(220) NOT NULL,
    event_date DATE NOT NULL,
    event_time TIME NOT NULL,
    banner_url VARCHAR(1024),
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE event_ticket_types (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    type VARCHAR(20) NOT NULL,
    price NUMERIC(14, 2) NOT NULL,
    capacity INTEGER NOT NULL,
    sold INTEGER NOT NULL DEFAULT 0,
    available INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_event_ticket_types_event FOREIGN KEY (event_id) REFERENCES ticket_events (id),
    CONSTRAINT uk_event_ticket_type UNIQUE (event_id, type),
    CONSTRAINT chk_event_ticket_price CHECK (price >= 0),
    CONSTRAINT chk_event_ticket_capacity CHECK (capacity > 0),
    CONSTRAINT chk_event_ticket_sold CHECK (sold >= 0),
    CONSTRAINT chk_event_ticket_available CHECK (available >= 0)
);

CREATE TABLE user_tickets (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    buyer_name VARCHAR(160) NOT NULL,
    buyer_email VARCHAR(180) NOT NULL,
    ticket_type VARCHAR(20) NOT NULL,
    price_paid NUMERIC(14, 2) NOT NULL,
    qr_code_value VARCHAR(1024) NOT NULL,
    ticket_reference VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(24) NOT NULL,
    purchased_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    scanned_by_worker_id VARCHAR(64),
    CONSTRAINT chk_user_ticket_price CHECK (price_paid >= 0)
);

CREATE TABLE ticket_payments (
    id VARCHAR(64) PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    buyer_name VARCHAR(160) NOT NULL,
    buyer_email VARCHAR(180) NOT NULL,
    ticket_type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    subtotal_amount NUMERIC(14, 2) NOT NULL,
    checkout_service_fee_amount NUMERIC(14, 2) NOT NULL,
    total_amount NUMERIC(14, 2) NOT NULL,
    status VARCHAR(24) NOT NULL,
    payment_provider VARCHAR(80) NOT NULL,
    payment_reference VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_ticket_payment_quantity CHECK (quantity > 0),
    CONSTRAINT chk_ticket_payment_subtotal CHECK (subtotal_amount >= 0),
    CONSTRAINT chk_ticket_payment_fee CHECK (checkout_service_fee_amount >= 0),
    CONSTRAINT chk_ticket_payment_total CHECK (total_amount >= 0)
);

CREATE TABLE ticket_withdrawals (
    id VARCHAR(64) PRIMARY KEY,
    owner_id VARCHAR(64) NOT NULL,
    gross_amount NUMERIC(14, 2) NOT NULL,
    service_fee_percent NUMERIC(5, 2) NOT NULL,
    service_fee_amount NUMERIC(14, 2) NOT NULL,
    net_amount NUMERIC(14, 2) NOT NULL,
    status VARCHAR(24) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    notes VARCHAR(700),
    CONSTRAINT chk_ticket_withdrawal_gross CHECK (gross_amount > 0),
    CONSTRAINT chk_ticket_withdrawal_fee_percent CHECK (service_fee_percent >= 0),
    CONSTRAINT chk_ticket_withdrawal_fee CHECK (service_fee_amount >= 0),
    CONSTRAINT chk_ticket_withdrawal_net CHECK (net_amount >= 0)
);

CREATE TABLE ticket_audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    owner_id VARCHAR(64),
    event_id VARCHAR(64),
    ticket_id VARCHAR(64),
    actor_id VARCHAR(64),
    action VARCHAR(80) NOT NULL,
    level VARCHAR(20) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    structured_metadata VARCHAR(3000) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_ticket_events_owner ON ticket_events (owner_id);
CREATE INDEX idx_ticket_events_status_date ON ticket_events (status, event_date);
CREATE INDEX idx_user_tickets_user ON user_tickets (user_id);
CREATE INDEX idx_user_tickets_event ON user_tickets (event_id);
CREATE INDEX idx_ticket_payments_event_status ON ticket_payments (event_id, status);
CREATE INDEX idx_ticket_withdrawals_owner_status ON ticket_withdrawals (owner_id, status);
CREATE INDEX idx_ticket_audit_logs_owner_created ON ticket_audit_logs (owner_id, created_at);
CREATE INDEX idx_ticket_audit_logs_event_created ON ticket_audit_logs (event_id, created_at);
