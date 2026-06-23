create table owner_payout_accounts (
    id bigserial primary key,
    owner_id bigint not null,
    business_id bigint not null,
    paypal_email varchar(255) not null,
    onboarding_token varchar(255) not null,
    onboarding_url varchar(2048) not null,
    status varchar(32) not null,
    created timestamp with time zone not null,
    updated timestamp with time zone not null,
    constraint uk_owner_payout_accounts_business_id unique (business_id)
);

create table transaction_withdrawals (
    id bigserial primary key,
    owner_id bigint not null,
    business_id bigint not null,
    gross_amount numeric(12, 2) not null,
    fee_amount numeric(12, 2) not null,
    fee_percent numeric(5, 2) not null,
    amount numeric(12, 2) not null,
    currency varchar(3) not null,
    transaction_count integer not null,
    paypal_email varchar(255) not null,
    status varchar(32) not null,
    requested_at timestamp with time zone not null,
    updated timestamp with time zone not null
);

alter table inventory_transactions add column transaction_withdrawal_id bigint;

create index idx_owner_payout_accounts_owner_id on owner_payout_accounts(owner_id);
create index idx_owner_payout_accounts_business_id on owner_payout_accounts(business_id);
create index idx_transaction_withdrawals_owner_id on transaction_withdrawals(owner_id);
create index idx_transaction_withdrawals_business_id on transaction_withdrawals(business_id);
create index idx_transaction_withdrawals_requested_at on transaction_withdrawals(requested_at);
create index idx_inventory_transactions_withdrawal_id on inventory_transactions(transaction_withdrawal_id);
