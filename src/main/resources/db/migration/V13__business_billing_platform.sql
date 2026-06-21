alter table businesses add column business_plan varchar(32) not null default 'FREE_TRIAL';
alter table businesses add column business_status varchar(32) not null default 'TRIAL';
alter table businesses add column trial_start_date timestamp;
alter table businesses add column trial_end_date timestamp;
alter table businesses add column subscription_start_date timestamp;
alter table businesses add column subscription_end_date timestamp;
alter table businesses add column current_billing_period_start_date timestamp;
alter table businesses add column current_billing_period_end_date timestamp;
alter table businesses add column paypal_subscription_id varchar(255);
alter table businesses add column paypal_subscription_token varchar(255);
alter table businesses add column paypal_plan_id varchar(255);
alter table businesses add column last_payment_date timestamp;
alter table businesses add column next_billing_date timestamp;

update businesses
set trial_start_date = created_date,
    trial_end_date = created_date + interval '14' day
where trial_start_date is null;

alter table bekina_faso_users
add column if not exists localization_country varchar(32) not null default 'SOUTH_AFRICA';

create table business_subscriptions (
    id bigserial primary key,
    business_id bigint not null,
    business_plan varchar(32) not null,
    billing_interval varchar(32) not null,
    term_years integer,
    amount numeric(12, 2) not null,
    currency varchar(3) not null,
    status varchar(32) not null,
    paypal_subscription_id varchar(255),
    paypal_subscription_token varchar(255),
    paypal_plan_id varchar(255),
    paypal_approval_url varchar(2048),
    period_start_date timestamp,
    period_end_date timestamp,
    created_date timestamp not null,
    modified_date timestamp not null,
    constraint fk_business_subscriptions_business
        foreign key (business_id)
        references businesses(id)
);