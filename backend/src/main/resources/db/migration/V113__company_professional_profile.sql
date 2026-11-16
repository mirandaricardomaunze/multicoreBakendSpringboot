-- Professional company profile (docs/EMPRESA_CADASTRO_PROFISSIONAL_SPEC.md):
-- legal identity, tax registration, structured address and extra contacts.
-- Every column is nullable, so existing companies keep their meaning.
-- "address" keeps holding the street line; the rest of the address goes to the new columns.

alter table companies add column if not exists trade_name       varchar(200);
alter table companies add column if not exists legal_form       varchar(30);
alter table companies add column if not exists registry_number  varchar(60);
alter table companies add column if not exists share_capital    numeric(19, 2);
alter table companies add column if not exists activity_license varchar(60);
alter table companies add column if not exists activity_code    varchar(20);
alter table companies add column if not exists vat_regime       varchar(20);
alter table companies add column if not exists tax_office       varchar(120);
alter table companies add column if not exists neighborhood     varchar(120);
alter table companies add column if not exists city             varchar(120);
alter table companies add column if not exists province         varchar(30);
alter table companies add column if not exists postal_code      varchar(20);
alter table companies add column if not exists secondary_phone  varchar(40);
alter table companies add column if not exists website          varchar(200);
alter table companies add column if not exists contact_name     varchar(120);
alter table companies add column if not exists contact_phone    varchar(40);
