create table billing_audit_logs (
    id bigserial primary key,
    business_id bigint,
    action varchar(64) not null,
    actor_username varchar(255),
    paypal_event_id varchar(255),
    paypal_subscription_id varchar(255),
    message varchar(2048),
    created_date timestamp not null,
    constraint fk_billing_audit_logs_business
        foreign key (business_id)
        references businesses(id)
);

create table paypal_webhook_events (
    id bigserial primary key,
    paypal_event_id varchar(255) not null unique,
    event_type varchar(255) not null,
    paypal_subscription_id varchar(255),
    status varchar(32) not null,
    failure_reason varchar(2048),
    raw_payload text,
    created_date timestamp not null,
    processed_date timestamp,
    constraint uk_paypal_webhook_events_event_id unique (paypal_event_id)
);

create index idx_billing_audit_logs_business_id on billing_audit_logs(business_id);
create index idx_billing_audit_logs_created_date on billing_audit_logs(created_date);
create index idx_paypal_webhook_events_subscription_id on paypal_webhook_events(paypal_subscription_id);
create index idx_paypal_webhook_events_event_type on paypal_webhook_events(event_type);
