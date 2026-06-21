alter table bekina_faso_users rename to tracker_users;

alter table beverage_transactions rename to inventory_transactions;

update privileges
set name = 'Owner'
where name = 'BekinaFasoOwner';

update privileges
set name = 'Worker'
where name = 'BekinafasiWorkers';
