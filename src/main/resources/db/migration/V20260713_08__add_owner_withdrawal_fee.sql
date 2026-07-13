ALTER TABLE business_withdrawals
    ADD COLUMN IF NOT EXISTS fee_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS net_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS withdrawal_fee_percent NUMERIC(5, 2) NOT NULL DEFAULT 0.00;

UPDATE business_withdrawals
SET net_amount = amount
WHERE net_amount = 0.00
  AND amount > 0.00;
