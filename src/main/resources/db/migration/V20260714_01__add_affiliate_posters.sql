CREATE TABLE affiliate_posters (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(1200),
    image_url VARCHAR(2048) NOT NULL,
    storage_object_name VARCHAR(1024) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(160) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_affiliate_posters_active_created
    ON affiliate_posters (active, created_at DESC);

CREATE INDEX idx_affiliate_posters_category
    ON affiliate_posters (category);
