ALTER TABLE ticket_event_promotions ADD COLUMN IF NOT EXISTS business_account_entry_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_ticket_event_promotions_account_entry ON ticket_event_promotions (business_account_entry_id);
