alter table businesses add column stripe_subscription_id varchar(255);
alter table businesses add column stripe_price_id varchar(255);

alter table business_subscriptions add column stripe_checkout_session_id varchar(255);
alter table business_subscriptions add column stripe_subscription_id varchar(255);
alter table business_subscriptions add column stripe_price_id varchar(255);
alter table business_subscriptions add column stripe_checkout_url varchar(2048);

create table stripe_webhook_events (
    id bigserial primary key,
    stripe_event_id varchar(255) not null unique,
    event_type varchar(255) not null,
    stripe_subscription_id varchar(255),
    status varchar(32) not null,
    failure_reason varchar(2048),
    raw_payload text,
    created_date timestamp not null,
    processed_date timestamp,
    constraint uk_stripe_webhook_events_event_id unique (stripe_event_id)
);

create index idx_business_subscriptions_stripe_checkout_session_id on business_subscriptions(stripe_checkout_session_id);
create index idx_business_subscriptions_stripe_subscription_id on business_subscriptions(stripe_subscription_id);
create index idx_stripe_webhook_events_subscription_id on stripe_webhook_events(stripe_subscription_id);
create index idx_stripe_webhook_events_event_type on stripe_webhook_events(event_type);
