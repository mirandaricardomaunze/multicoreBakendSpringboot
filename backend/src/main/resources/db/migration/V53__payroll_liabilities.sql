-- RH B5: o dinheiro retido e não entregue passa a existir como dívida.
-- Ver docs/RH_COMPLETO_SPEC.md §B5.
--
-- Até aqui o IRPS retido ao trabalhador e o INSS das duas partes eram calculados, impressos no mapa
-- fiscal e DESAPARECIAM. Só o líquido saía da tesouraria: o dinheiro do Estado ficava na conta da
-- empresa sem estar marcado como dívida, e ninguém era avisado do prazo. Uma empresa que gasta o
-- que reteve não descobre o buraco no mês em que o gasta — descobre no dia em que tem de entregar.

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. Valores legais configuráveis (§6). NÃO É A IA QUE DECIDE O NÚMERO.
-- ─────────────────────────────────────────────────────────────────────────────
-- Prazos de entrega, direito a férias por antiguidade e aviso prévio vêm da Lei do Trabalho e da
-- legislação fiscal, que mudam. Molde do payroll_tax_configs: por empresa, com vigência e base
-- legal registada. Sem valores por omissão — o que não estiver configurado é dito como não
-- configurado, em vez de ser adivinhado.
create table if not exists hr_policy_configs (
    id                          bigserial primary key,
    company_id                  bigint       not null references companies (id),
    name                        varchar(120) not null,
    effective_from              date         not null,
    effective_to                date,

    -- Direito anual de férias por antiguidade (RHC-71). Nulo = por confirmar; nesse caso o sistema
    -- mantém o valor histórico de 22 dias e DIZ que é um valor por omissão.
    vacation_days_year_1        integer,
    vacation_days_year_2        integer,
    vacation_days_year_3_plus   integer,

    -- Dia do mês SEGUINTE ao período em que a retenção tem de estar entregue (RHC-52).
    irps_delivery_day           integer,
    inss_delivery_day           integer,

    -- Aviso prévio, em dias, por iniciativa de cada parte (B3).
    notice_days_employee        integer,
    notice_days_employer        integer,

    legal_basis                 varchar(500),
    active                      boolean      not null default true,
    created_at                  timestamp    not null,
    updated_at                  timestamp,
    created_by                  varchar(255)
);

create index if not exists idx_hr_policy_configs_lookup
    on hr_policy_configs (company_id, effective_from desc);

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Retenções por entregar
-- ─────────────────────────────────────────────────────────────────────────────
create table if not exists payroll_liabilities (
    id                 bigserial primary key,
    company_id         bigint         not null references companies (id),
    ref_year           integer        not null,
    ref_month          integer        not null,
    liability_type     varchar(30)    not null,   -- IRPS | INSS_TRABALHADOR | INSS_PATRONAL
    amount             numeric(19, 2) not null,
    -- Nulo quando o prazo legal ainda não foi configurado. A obrigação nasce na mesma: não saber o
    -- prazo não é razão para perder o rasto do dinheiro.
    due_date           date,
    status             varchar(20)    not null,   -- POR_ENTREGAR | ENTREGUE
    payment_date       date,
    payment_reference  varchar(120),
    delivered_by       varchar(120),
    created_at         timestamp      not null,
    updated_at         timestamp,
    created_by         varchar(255)
);

-- Uma obrigação por empresa, período e tipo: é assim que se entrega ao Estado, e é assim que tem
-- de ser contada. Duas linhas do mesmo IRPS de Agosto seriam duas dívidas para o mesmo pagamento.
create unique index if not exists uk_payroll_liabilities_period_type
    on payroll_liabilities (company_id, ref_year, ref_month, liability_type);

-- "O que é que ainda devo ao Estado, e para quando?" — a consulta do sino e do painel.
create index if not exists idx_payroll_liabilities_pending
    on payroll_liabilities (company_id, status, due_date);
