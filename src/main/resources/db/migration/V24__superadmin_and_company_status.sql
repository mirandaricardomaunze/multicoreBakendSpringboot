-- Superadmin (dono da plataforma) e estado da empresa.
-- `platform_admin` é ortogonal aos papéis por-empresa: marca contas que gerem a plataforma inteira.
-- `companies.active` permite suspender uma empresa (bloqueia o login dos seus utilizadores) sem
-- apagar dados. Defaults preservam o comportamento actual (todas activas; ninguém é superadmin).
alter table app_users add column platform_admin boolean not null default false;
alter table companies add column active boolean not null default true;
