-- Login sessions survive a backend restart and are shared by every instance
-- (docs/SEGURANCA_CONTAS_E_SEGREDOS_SPEC.md, SC-33..SC-35). Only the SHA-256 of the bearer token is stored:
-- reading this table does not let anyone use a session.
create table if not exists auth_sessions (
    token_hash varchar(64)              not null primary key,
    username   varchar(255)             not null,
    issued_at  timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    restricted boolean                  not null default false,
    created_at timestamp                not null,
    updated_at timestamp,
    created_by varchar(255)
);

create index if not exists idx_auth_sessions_username on auth_sessions (username, issued_at);
create index if not exists idx_auth_sessions_expires on auth_sessions (expires_at);
