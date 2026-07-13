ALTER TABLE business_withdrawals
    ADD COLUMN IF NOT EXISTS provider VARCHAR(32),
    ADD COLUMN IF NOT EXISTS provider_batch_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS provider_status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS payout_amount NUMERIC(14, 2),
    ADD COLUMN IF NOT EXISTS payout_currency VARCHAR(3);

CREATE UNIQUE INDEX IF NOT EXISTS uk_business_withdrawals_provider_batch
    ON business_withdrawals (provider, provider_batch_id);

CREATE INDEX IF NOT EXISTS idx_business_withdrawals_provider_status
    ON business_withdrawals (provider, provider_status);
