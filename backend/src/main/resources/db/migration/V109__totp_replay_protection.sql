-- Último intervalo TOTP aceite: cada código da dupla autenticação entra uma só vez.
alter table app_users add column if not exists last_totp_step bigint;
