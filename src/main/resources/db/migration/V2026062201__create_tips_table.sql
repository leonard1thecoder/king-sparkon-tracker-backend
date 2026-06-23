create table tip_withdrawals (
    id bigserial primary key,
    worker_id bigint not null,
    owner_id bigint not null,
    amount numeric(12, 2) not null,
    currency varchar(3) not null,
    tip_count integer not null,
    paypal_email varchar(255) not null,
    status varchar(32) not null,
    requested_at timestamp with time zone not null,
    updated timestamp with time zone not null,
    constraint fk_tip_withdrawals_worker foreign key (worker_id) references tracker_users(id),
    constraint fk_tip_withdrawals_owner foreign key (owner_id) references tracker_users(id)
);

create table tips (
    id bigserial primary key,
    worker_id bigint not null,
    tip_amount numeric(12, 2) not null,
    created timestamp with time zone not null,
    updated timestamp with time zone not null,
    status varchar(16) not null,
    payment_reference varchar(255),
    withdrawal_id bigint,
    constraint fk_tips_worker foreign key (worker_id) references tracker_users(id),
    constraint fk_tips_withdrawal foreign key (withdrawal_id) references tip_withdrawals(id)
);

create table worker_payout_accounts (
    id bigserial primary key,
    worker_id bigint not null,
    owner_id bigint not null,
    paypal_email varchar(255) not null,
    onboarding_token varchar(255) not null,
    onboarding_url varchar(2048) not null,
    status varchar(32) not null,
    created timestamp with time zone not null,
    updated timestamp with time zone not null,
    constraint uk_worker_payout_accounts_worker_id unique (worker_id),
    constraint fk_worker_payout_accounts_worker foreign key (worker_id) references tracker_users(id),
    constraint fk_worker_payout_accounts_owner foreign key (owner_id) references tracker_users(id)
);

create index idx_tips_worker_id on tips(worker_id);
create index idx_tips_status on tips(status);
create index idx_tips_created on tips(created);
create index idx_tips_withdrawal_id on tips(withdrawal_id);
create index idx_tip_withdrawals_worker_id on tip_withdrawals(worker_id);
create index idx_tip_withdrawals_owner_id on tip_withdrawals(owner_id);
create index idx_worker_payout_accounts_worker_id on worker_payout_accounts(worker_id);
create index idx_worker_payout_accounts_owner_id on worker_payout_accounts(owner_id);
