alter table tracker_users add column affiliate_code varchar(64);
alter table tracker_users add column affiliate_promotion_url varchar(2048);
alter table tracker_users add column affiliate_qr_code_url varchar(2048);
alter table tracker_users add column affiliate_paypal_link varchar(2048);
alter table tracker_users add column affiliate_joined_at timestamp;

create unique index uk_tracker_users_affiliate_code
    on tracker_users(affiliate_code);

alter table businesses add column affiliate_user_id bigint;
alter table businesses add column affiliate_code varchar(64);

alter table businesses
    add constraint fk_businesses_affiliate_user
    foreign key (affiliate_user_id)
    references tracker_users(id);

create index idx_businesses_affiliate_user_id on businesses(affiliate_user_id);
create index idx_businesses_affiliate_code on businesses(affiliate_code);

create table affiliate_withdrawals (
    id bigserial primary key,
    affiliate_user_id bigint not null,
    amount numeric(12, 2) not null,
    currency varchar(3) not null,
    commission_count integer not null,
    paypal_link varchar(2048) not null,
    status varchar(32) not null,
    requested_at timestamp not null,
    updated timestamp not null,
    constraint fk_affiliate_withdrawals_affiliate_user
        foreign key (affiliate_user_id)
        references tracker_users(id)
);

create index idx_affiliate_withdrawals_affiliate_user_id
    on affiliate_withdrawals(affiliate_user_id);

create index idx_affiliate_withdrawals_affiliate_status_requested
    on affiliate_withdrawals(affiliate_user_id, status, requested_at desc);

create table affiliate_commissions (
    id bigserial primary key,
    affiliate_user_id bigint not null,
    business_id bigint not null,
    subscription_id bigint not null,
    withdrawal_id bigint,
    gross_amount numeric(12, 2) not null,
    commission_rate_percent numeric(5, 2) not null,
    commission_amount numeric(12, 2) not null,
    currency varchar(3) not null,
    status varchar(32) not null,
    earned_at timestamp not null,
    updated timestamp not null,
    constraint fk_affiliate_commissions_affiliate_user
        foreign key (affiliate_user_id)
        references tracker_users(id),
    constraint fk_affiliate_commissions_business
        foreign key (business_id)
        references businesses(id),
    constraint fk_affiliate_commissions_subscription
        foreign key (subscription_id)
        references business_subscriptions(id),
    constraint fk_affiliate_commissions_withdrawal
        foreign key (withdrawal_id)
        references affiliate_withdrawals(id),
    constraint uk_affiliate_commissions_subscription
        unique (subscription_id)
);

create index idx_affiliate_commissions_affiliate_user_id
    on affiliate_commissions(affiliate_user_id);

create index idx_affiliate_commissions_affiliate_status_earned
    on affiliate_commissions(affiliate_user_id, status, earned_at asc);

create index idx_affiliate_commissions_business_id
    on affiliate_commissions(business_id);

create index idx_affiliate_commissions_withdrawal_id
    on affiliate_commissions(withdrawal_id);
