-- Gestão de fornecedores: contacto e soft-delete (activo).
alter table suppliers add column if not exists phone          varchar(40);
alter table suppliers add column if not exists contact_person varchar(255);
alter table suppliers add column if not exists active         boolean not null default true;

create index if not exists idx_supplier_company_active on suppliers (company_id, active);
