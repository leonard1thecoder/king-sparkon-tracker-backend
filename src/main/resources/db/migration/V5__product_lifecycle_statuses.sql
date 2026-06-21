alter table products add column status varchar(32) not null default 'CREATED';

alter table product_barcodes add column status varchar(32) not null default 'NOT_CLAIMED';
