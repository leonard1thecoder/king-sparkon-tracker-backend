alter table products add column returnable_enabled boolean not null default false;
alter table products add column returnable_price numeric(12, 2) not null default 0.00;

alter table products add column night_shift_enabled boolean not null default false;
alter table products add column night_shift_price numeric(12, 2) not null default 0.00;
alter table products add column night_shift_start_time time;
alter table products add column night_shift_end_time time;

update products
set returnable_enabled = bottle_returnable,
    returnable_price = case when bottle_returnable = true then 3.00 else 0.00 end;
