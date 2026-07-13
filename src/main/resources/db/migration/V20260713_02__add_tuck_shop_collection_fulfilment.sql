ALTER TABLE inventory_transactions
    ADD COLUMN IF NOT EXISTS customer_id BIGINT,
    ADD COLUMN IF NOT EXISTS fulfilment_status VARCHAR(48) NOT NULL DEFAULT 'NOT_REQUIRED',
    ADD COLUMN IF NOT EXISTS collection_token VARCHAR(96),
    ADD COLUMN IF NOT EXISTS collection_ready_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS collected_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS prepared_by_worker_id BIGINT;

UPDATE inventory_transactions
SET fulfilment_status = 'NOT_REQUIRED'
WHERE fulfilment_status IS NULL OR fulfilment_status = '';

CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_transactions_collection_token
    ON inventory_transactions (collection_token);

CREATE INDEX IF NOT EXISTS idx_inventory_transactions_customer_date
    ON inventory_transactions (customer_id, date);

CREATE INDEX IF NOT EXISTS idx_inventory_transactions_business_fulfilment
    ON inventory_transactions (business_id, payment_status, fulfilment_status);
