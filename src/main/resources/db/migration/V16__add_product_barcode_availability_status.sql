ALTER TABLE product_barcodes
    ADD COLUMN availability_status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE';

UPDATE product_barcodes
SET availability_status = 'SOLD'
WHERE barcode IN (
    SELECT barcode
    FROM transaction_item_barcodes
);