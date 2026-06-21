create table transaction_item_barcodes (
	transaction_item_id bigint not null,
	barcode_order integer not null,
	barcode varchar(255) not null,
	constraint pk_transaction_item_barcodes primary key (transaction_item_id, barcode_order),
	constraint fk_transaction_item_barcodes_item foreign key (transaction_item_id) references transaction_items (id)
);

create index idx_transaction_item_barcodes_item_id on transaction_item_barcodes (transaction_item_id);
