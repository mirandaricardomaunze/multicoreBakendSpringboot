-- Sessões do portal web do gestor (docs/PORTAL_GESTOR_SPEC.md §3.1, PG-03): expiram 12 h depois do login
-- ou 30 min depois do último pedido. A coluna distingue-as das sessões do desktop, que não mudam.
alter table auth_sessions add column if not exists portal boolean not null default false;
