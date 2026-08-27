-- Explicit tenant membership for every application user.
create table if not exists app_user_companies (
    user_id bigint not null,
    company_id bigint not null,
    constraint uk_app_user_company unique (user_id, company_id),
    constraint fk_app_user_companies_user foreign key (user_id) references app_users(id),
    constraint fk_app_user_companies_company foreign key (company_id) references companies(id)
);

create index if not exists idx_app_user_companies_company
    on app_user_companies(company_id);

-- Preserve access during upgrade. Administrators receive all companies;
-- other existing users receive the first company and can be adjusted later.
insert into app_user_companies (user_id, company_id)
select u.id, c.id
from app_users u
cross join companies c
where u.role = 'ADMIN'
on conflict do nothing;

insert into app_user_companies (user_id, company_id)
select u.id, c.id
from app_users u
cross join lateral (select id from companies order by id limit 1) c
where u.role <> 'ADMIN'
on conflict do nothing;
