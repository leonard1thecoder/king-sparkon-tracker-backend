alter table products add column if not exists product_image_url varchar(2048);

create index if not exists idx_products_tuck_shop_available on products (business_id, status, stock_quantity);
