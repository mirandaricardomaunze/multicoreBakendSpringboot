-- Explicit company visibility for shared master data.
create table if not exists client_companies (
    client_id bigint not null references clients(id),
    company_id bigint not null references companies(id),
    constraint uk_client_company unique (client_id, company_id)
);

create table if not exists product_companies (
    product_id bigint not null references products(id),
    company_id bigint not null references companies(id),
    constraint uk_product_company unique (product_id, company_id)
);

create table if not exists product_category_companies (
    category_id bigint not null references product_categories(id),
    company_id bigint not null references companies(id),
    constraint uk_product_category_company unique (category_id, company_id)
);

create table if not exists tax_rate_companies (
    tax_rate_id bigint not null references tax_rates(id),
    company_id bigint not null references companies(id),
    constraint uk_tax_rate_company unique (tax_rate_id, company_id)
);

insert into client_companies (client_id, company_id)
select distinct client_id, company_id from invoices where company_id is not null
on conflict do nothing;
insert into client_companies (client_id, company_id)
select distinct client_id, company_id from customer_orders where company_id is not null
on conflict do nothing;
insert into client_companies (client_id, company_id)
select c.id, (select id from companies order by id limit 1)
from clients c
where not exists (select 1 from client_companies cc where cc.client_id = c.id)
on conflict do nothing;

insert into product_companies (product_id, company_id)
select distinct s.product_id, w.company_id from stocks s join warehouses w on w.id = s.warehouse_id
on conflict do nothing;
insert into product_companies (product_id, company_id)
select distinct il.product_id, i.company_id from invoice_lines il join invoices i on i.id = il.invoice_id
where i.company_id is not null
on conflict do nothing;
insert into product_companies (product_id, company_id)
select p.id, (select id from companies order by id limit 1)
from products p
where not exists (select 1 from product_companies pc where pc.product_id = p.id)
on conflict do nothing;

insert into product_category_companies (category_id, company_id)
select distinct p.category_id, pc.company_id
from products p join product_companies pc on pc.product_id = p.id
where p.category_id is not null
on conflict do nothing;
insert into product_category_companies (category_id, company_id)
select c.id, (select id from companies order by id limit 1)
from product_categories c
where not exists (select 1 from product_category_companies cc where cc.category_id = c.id)
on conflict do nothing;

-- Fiscal rates are shared explicitly on upgrade; administrators can later
-- remove a company association or create company-specific configurations.
insert into tax_rate_companies (tax_rate_id, company_id)
select t.id, c.id from tax_rates t cross join companies c
on conflict do nothing;

-- Operational records always have one owning company.
alter table if exists crm_tickets add column if not exists company_id bigint;
update crm_tickets
set company_id = (select id from companies order by id limit 1)
where company_id is null;
alter table if exists crm_tickets alter column company_id set not null;
alter table if exists crm_tickets
    add constraint fk_crm_tickets_company foreign key (company_id) references companies(id);

alter table if exists approval_requests add column if not exists company_id bigint;
update approval_requests ar
set company_id = i.company_id
from invoices i
where ar.document_type = 'INVOICE' and ar.document_id = i.id and ar.company_id is null;
update approval_requests ar
set company_id = e.company_id
from expense_claims ec join employees e on e.id = ec.employee_id
where ar.document_type = 'EXPENSE' and ar.document_id = ec.id and ar.company_id is null;
update approval_requests
set company_id = (select id from companies order by id limit 1)
where company_id is null;
alter table if exists approval_requests alter column company_id set not null;
alter table if exists approval_requests
    add constraint fk_approval_requests_company foreign key (company_id) references companies(id);

create index if not exists idx_client_companies_company on client_companies(company_id);
create index if not exists idx_product_companies_company on product_companies(company_id);
create index if not exists idx_product_category_companies_company on product_category_companies(company_id);
create index if not exists idx_tax_rate_companies_company on tax_rate_companies(company_id);
create index if not exists idx_crm_tickets_company on crm_tickets(company_id);
create index if not exists idx_approval_requests_company on approval_requests(company_id);
