CREATE TABLE IF NOT EXISTS business_withdrawals (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    payout_method VARCHAR(40) NOT NULL,
    payout_destination VARCHAR(320),
    notes VARCHAR(700),
    status VARCHAR(24) NOT NULL DEFAULT 'REQUESTED',
    ledger_entry_id BIGINT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_business_withdrawals_business
        FOREIGN KEY (business_id) REFERENCES businesses(id),
    CONSTRAINT fk_business_withdrawals_owner
        FOREIGN KEY (owner_id) REFERENCES tracker_users(id),
    CONSTRAINT fk_business_withdrawals_ledger
        FOREIGN KEY (ledger_entry_id) REFERENCES business_account_ledger_entries(id)
);

CREATE INDEX IF NOT EXISTS idx_business_withdrawals_business_requested
    ON business_withdrawals (business_id, requested_at DESC);

CREATE INDEX IF NOT EXISTS idx_business_withdrawals_status
    ON business_withdrawals (status);

CREATE INDEX IF NOT EXISTS idx_business_account_ledger_source
    ON business_account_ledger_entries (business_id, entry_type, provider_reference);
