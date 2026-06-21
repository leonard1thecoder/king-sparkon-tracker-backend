alter table product_barcodes add column referencee varchar(255);

update product_barcodes
set status = 'CLAIMED'
where status = 'NOT_CLAIMED'
and product_id in (
	select id
	from products
	where bottle_returnable = false
);

create index idx_product_barcodes_referencee on product_barcodes (referencee);
