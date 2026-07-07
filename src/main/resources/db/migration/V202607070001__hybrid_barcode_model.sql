CREATE TABLE IF NOT EXISTS barcode_catalog (
    id BIGSERIAL PRIMARY KEY,
    barcode VARCHAR(128) NOT NULL,
    product_name VARCHAR(255),
    brand VARCHAR(160),
    category VARCHAR(120),
    image_url VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_barcode_catalog_barcode UNIQUE (barcode)
);

ALTER TABLE products ADD COLUMN IF NOT EXISTS product_barcode VARCHAR(128);
ALTER TABLE products ADD COLUMN IF NOT EXISTS barcode_catalog_id BIGINT;

INSERT INTO barcode_catalog (barcode, product_name, image_url)
SELECT DISTINCT pb.barcode, p.name, p.product_image_url
FROM product_barcodes pb
JOIN products p ON p.id = pb.product_id
WHERE pb.barcode IS NOT NULL
ON CONFLICT (barcode) DO NOTHING;

UPDATE products p
SET product_barcode = first_barcode.barcode,
    barcode_catalog_id = bc.id
FROM (
    SELECT DISTINCT ON (product_id) product_id, barcode
    FROM product_barcodes
    WHERE barcode IS NOT NULL
    ORDER BY product_id, id ASC
) first_barcode
JOIN barcode_catalog bc ON bc.barcode = first_barcode.barcode
WHERE p.id = first_barcode.product_id
  AND p.product_barcode IS NULL;

ALTER TABLE product_barcodes ADD COLUMN IF NOT EXISTS unit_code VARCHAR(120);

UPDATE product_barcodes
SET unit_code = 'KST-UNIT-' || LPAD(id::TEXT, 12, '0')
WHERE unit_code IS NULL;

ALTER TABLE product_barcodes ALTER COLUMN unit_code SET NOT NULL;

ALTER TABLE products
    ADD CONSTRAINT fk_products_barcode_catalog
    FOREIGN KEY (barcode_catalog_id)
    REFERENCES barcode_catalog (id);

ALTER TABLE products
    ADD CONSTRAINT uk_products_business_product_barcode
    UNIQUE (business_id, product_barcode);

ALTER TABLE product_barcodes DROP CONSTRAINT IF EXISTS uk_product_barcodes_barcode;
ALTER TABLE product_barcodes DROP CONSTRAINT IF EXISTS product_barcodes_barcode_key;

ALTER TABLE product_barcodes
    ADD CONSTRAINT uk_product_barcodes_unit_code
    UNIQUE (unit_code);

CREATE INDEX IF NOT EXISTS idx_products_product_barcode ON products (product_barcode);
CREATE INDEX IF NOT EXISTS idx_product_barcodes_barcode ON product_barcodes (barcode);
