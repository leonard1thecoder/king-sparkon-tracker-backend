alter table businesses add column description varchar(2000);

alter table subscribers add column affiliate_registered boolean not null default false;

alter table promotions add column audience varchar(48) not null default 'ALL_SUBSCRIBERS';

alter table inventory_transactions add column payment_contact varchar(320);

create index idx_subscribers_affiliate_registered on subscribers (subscriber_type, affiliate_registered);
create index idx_promotions_audience on promotions (audience);
