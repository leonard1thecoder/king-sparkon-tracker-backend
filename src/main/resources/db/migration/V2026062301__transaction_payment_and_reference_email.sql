alter table inventory_transactions add column payment_type varchar(32);
alter table inventory_transactions add column payment_status varchar(32) not null default 'NOT_REQUIRED';
alter table inventory_transactions add column payment_email varchar(255);
alter table inventory_transactions add column payment_reference varchar(255);
alter table inventory_transactions add column payment_url varchar(2048);

alter table product_barcodes rename column referencee to reference_email;

create index idx_inventory_transactions_payment_type on inventory_transactions (payment_type);
create index idx_inventory_transactions_payment_status on inventory_transactions (payment_status);
create index idx_inventory_transactions_payment_reference on inventory_transactions (payment_reference);
