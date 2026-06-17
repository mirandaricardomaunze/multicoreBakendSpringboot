alter table if exists treasury_accounts
    add column if not exists company_id bigint;

update treasury_accounts
set company_id = (select id from companies order by id limit 1)
where company_id is null;

alter table if exists treasury_accounts
    alter column company_id set not null;

alter table if exists treasury_accounts
    add constraint fk_treasury_accounts_company
    foreign key (company_id) references companies(id);

create index if not exists idx_treasury_accounts_company
    on treasury_accounts(company_id);
