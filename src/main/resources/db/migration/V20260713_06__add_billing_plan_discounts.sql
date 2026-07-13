CREATE TABLE IF NOT EXISTS billing_plan_discounts (
    id BIGSERIAL PRIMARY KEY,
    business_plan VARCHAR(32) NOT NULL,
    discount_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    label VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    updated_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_billing_plan_discounts_plan UNIQUE (business_plan),
    CONSTRAINT chk_billing_plan_discount_percent CHECK (discount_percent >= 0 AND discount_percent <= 90),
    CONSTRAINT chk_billing_plan_discount_paid_plan CHECK (business_plan IN ('PLUS', 'PRO')),
    CONSTRAINT chk_billing_plan_discount_window CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at)
);

CREATE INDEX IF NOT EXISTS idx_billing_plan_discounts_active_window
    ON billing_plan_discounts(active, starts_at, ends_at);
