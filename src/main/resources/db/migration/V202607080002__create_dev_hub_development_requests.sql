CREATE TABLE IF NOT EXISTS dev_hub_development_requests (
    id BIGSERIAL PRIMARY KEY,
    client_name VARCHAR(160) NOT NULL,
    email_address VARCHAR(180) NOT NULL,
    phone_number VARCHAR(80),
    company_name VARCHAR(180),
    project_type VARCHAR(120) NOT NULL,
    title VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    budget_range VARCHAR(120),
    timeline VARCHAR(120),
    currency VARCHAR(3) NOT NULL DEFAULT 'ZAR',
    estimated_min_price NUMERIC(14, 2) NOT NULL DEFAULT 0,
    estimated_max_price NUMERIC(14, 2) NOT NULL DEFAULT 0,
    ai_development_plan TEXT NOT NULL,
    ai_automated_response TEXT NOT NULL,
    decision_reason TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'AI_QUOTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dev_hub_requests_status ON dev_hub_development_requests(status);
CREATE INDEX IF NOT EXISTS idx_dev_hub_requests_created_at ON dev_hub_development_requests(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dev_hub_requests_email ON dev_hub_development_requests(email_address);
