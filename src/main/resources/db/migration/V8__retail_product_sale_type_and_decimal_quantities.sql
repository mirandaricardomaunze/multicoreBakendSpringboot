alter table products
    add column if not exists sale_type varchar(20) not null default 'UNIT';

alter table products
    add column if not exists stock_tracked boolean not null default true;

alter table invoice_lines
    alter column quantity type numeric(12,3) using quantity::numeric(12,3);

alter table customer_order_lines
    alter column quantity type numeric(12,3) using quantity::numeric(12,3);
