alter table tracker_users add column physical_address varchar(1024);
alter table tracker_users add column cellphone_number varchar(32);
alter table tracker_users add column job_title varchar(120);
alter table tracker_users add column onboarding_completed boolean not null default false;
alter table tracker_users add column tip_qr_code_enabled boolean not null default false;
alter table tracker_users add column tip_qr_code_url varchar(2048);

create index idx_tracker_users_onboarding_completed on tracker_users(onboarding_completed);
create index idx_tracker_users_tip_qr_code_enabled on tracker_users(tip_qr_code_enabled);
