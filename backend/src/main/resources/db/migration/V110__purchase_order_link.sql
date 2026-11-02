-- Liga a compra à encomenda que a originou: a recepção passa a criar a dívida ao fornecedor.
alter table purchases add column if not exists purchase_order_id bigint;
alter table purchases add constraint fk_purchases_purchase_order
    foreign key (purchase_order_id) references purchase_orders (id);
create index if not exists idx_purchases_purchase_order on purchases (purchase_order_id);
