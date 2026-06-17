-- A user can have a different authorization role in each company.
alter table app_user_companies
    add column if not exists role varchar(30);

update app_user_companies access
set role = (
    select app_user.role
    from app_users app_user
    where app_user.id = access.user_id
)
where access.role is null;

alter table app_user_companies
    alter column role set not null;

alter table app_user_companies
    add constraint pk_app_user_companies primary key (user_id, company_id);
