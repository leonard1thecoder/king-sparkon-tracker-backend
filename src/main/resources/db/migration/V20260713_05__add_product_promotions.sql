CREATE TABLE IF NOT EXISTS product_promotions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    business_id BIGINT NOT NULL,
    promotion_price NUMERIC(12,2) NOT NULL,
    business_account_entry_id BIGINT,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_promotions_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_product_promotions_business FOREIGN KEY (business_id) REFERENCES businesses(id),
    CONSTRAINT fk_product_promotions_account_entry FOREIGN KEY (business_account_entry_id) REFERENCES business_account_ledger_entries(id)
);

CREATE INDEX IF NOT EXISTS idx_product_promotions_active_window
    ON product_promotions(active, starts_at, ends_at);

CREATE INDEX IF NOT EXISTS idx_product_promotions_business_created
    ON product_promotions(business_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_product_promotions_product_active
    ON product_promotions(product_id, active);
