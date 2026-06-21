update product_barcodes barcode
set status = 'NOT_CLAIMABLE'
where status = 'CLAIMED'
and exists (
    select 1
    from products product
    where product.id = barcode.product_id
    and product.returnable_enabled = false
);
