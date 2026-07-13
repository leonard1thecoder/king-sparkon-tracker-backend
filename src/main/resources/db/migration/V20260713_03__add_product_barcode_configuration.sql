CREATE TABLE IF NOT EXISTS product_barcode_configurations (
    product_id BIGINT PRIMARY KEY,
    barcode_mode VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_barcode_configuration_product
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT chk_product_barcode_mode
        CHECK (barcode_mode IN ('BRANDED', 'AUTO_GENERATED'))
);

INSERT INTO product_barcode_configurations (product_id, barcode_mode, created_at, updated_at)
SELECT p.id,
       CASE
           WHEN p.product_barcode IS NULL OR TRIM(p.product_barcode) = '' THEN 'AUTO_GENERATED'
           ELSE 'BRANDED'
       END,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM products p
WHERE NOT EXISTS (
    SELECT 1
    FROM product_barcode_configurations configuration
    WHERE configuration.product_id = p.id
);

CREATE INDEX IF NOT EXISTS idx_product_barcode_configurations_mode
    ON product_barcode_configurations (barcode_mode);
