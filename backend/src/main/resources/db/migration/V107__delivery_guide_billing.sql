-- Liga a guia de remessa à fatura que a cobra. A fatura de guia não move stock.
alter table delivery_guides add column if not exists invoice_id bigint;
alter table delivery_guides add column if not exists invoice_number varchar(50);
alter table invoices add column if not exists delivery_guide_id bigint;
create index if not exists idx_delivery_guides_invoice on delivery_guides (company_id, invoice_id);
