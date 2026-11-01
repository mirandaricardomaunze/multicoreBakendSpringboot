-- Troca de senha obrigatória depois de uma reposição, e segredo da dupla autenticação.
alter table app_users add column if not exists password_change_required boolean default false not null;
alter table app_users add column if not exists totp_secret varchar(255);
