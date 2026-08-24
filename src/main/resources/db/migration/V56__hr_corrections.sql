-- RH B8: correcções ao que já existia. Ver docs/RH_COMPLETO_SPEC.md §B8.
--
-- Furos pequenos, todos confirmados no código, que não justificavam bloco próprio mas custavam
-- dinheiro ou credibilidade a cada mês que passavam.

-- ─────────────────────────────────────────────────────────────────────────────
-- 8.6 — Fecho de período da folha
-- ─────────────────────────────────────────────────────────────────────────────
-- `processMonthlyPayroll` corria para qualquer mês, sempre. Um mês já pago, já entregue ao Estado e
-- já contabilizado continuava a aceitar recibos novos — e cada recibo novo nesse mês desalinhava a
-- retenção declarada (§B5) sem nada avisar.
create table if not exists payroll_periods (
    id             bigserial primary key,
    company_id     bigint       not null references companies (id),
    ref_year       integer      not null,
    ref_month      integer      not null,
    status         varchar(20)  not null,   -- ABERTO | FECHADO
    closed_by      varchar(120),
    closed_at      timestamp,
    reopen_reason  varchar(500),
    created_at     timestamp    not null,
    updated_at     timestamp,
    created_by     varchar(255)
);

create unique index if not exists uk_payroll_periods
    on payroll_periods (company_id, ref_year, ref_month);

-- ─────────────────────────────────────────────────────────────────────────────
-- 8.8 — Documentos do colaborador com validade
-- ─────────────────────────────────────────────────────────────────────────────
-- O DIRE de um trabalhador estrangeiro caducar sem aviso é multa. E não havia sítio nenhum no
-- sistema onde essa data existisse.
create table if not exists employee_documents (
    id            bigserial primary key,
    company_id    bigint       not null references companies (id),
    employee_id   bigint       not null references employees (id),
    document_type varchar(40)  not null,   -- BI | DIRE | PASSAPORTE | NUIT | CERTIFICADO | OUTRO
    document_number varchar(80),
    issue_date    date,
    -- Nulo = não caduca (BI vitalício, NUIT). Não é o mesmo que "sem data preenchida", e por isso
    -- o alerta só olha para os que têm data.
    expiry_date   date,
    notes         varchar(500),
    created_at    timestamp    not null,
    updated_at    timestamp,
    created_by    varchar(255)
);

create index if not exists idx_employee_documents_expiry
    on employee_documents (company_id, expiry_date);

create index if not exists idx_employee_documents_employee
    on employee_documents (company_id, employee_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- 8.7 — Ficheiro de pagamento bancário
-- ─────────────────────────────────────────────────────────────────────────────
-- Numa folha de 30 pessoas, pagava-se uma a uma. Um ficheiro de pagamento precisa da conta — e a
-- ficha do colaborador não tinha onde a guardar, pelo que o ficheiro seria inútil sem estas duas
-- colunas. Nullable: quem paga em numerário continua exactamente como estava.
alter table employees add column if not exists bank_name varchar(120);
alter table employees add column if not exists bank_account varchar(60);
