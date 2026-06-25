alter table invoices
    add column if not exists customer_name varchar(120);
