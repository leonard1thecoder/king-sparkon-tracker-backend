alter table businesses
    add column if not exists qr_code_url varchar(2048);

alter table inventory_transactions
    add column if not exists payment_qr_code_url varchar(2048);
