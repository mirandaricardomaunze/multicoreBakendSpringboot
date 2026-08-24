-- RH: acréscimos de hora extra, configuráveis por empresa e com base legal registada.
-- Ver docs/RH_COMPLETO_SPEC.md §B2 e §6. Molde da tabela payroll_tax_configs.
--
-- NÃO HÁ VALORES POR OMISSÃO, e é deliberado. A lei laboral moçambicana tem acréscimos distintos
-- por tipo de hora, e quais se aplicam a esta empresa é decisão do contabilista dela — não do
-- sistema. Sem configuração em vigor, o recibo recusa-se a valorizar horas extra e diz porquê.
-- Um acréscimo escrito à sorte seria o pior resultado possível: parece certo, paga mal, e ninguém
-- repara até alguém reclamar.
--
-- A coluna legal_basis existe para que quem audita — ou o contabilista seguinte — saiba contra o
-- quê conferir, em vez de encontrar três números sem proveniência.

create table if not exists overtime_rate_configs (
    id                  bigserial primary key,
    company_id          bigint        not null references companies (id),
    name                varchar(120)  not null,
    effective_from      date          not null,
    effective_to        date,
    -- Multiplicadores sobre o valor/hora normal. Sem defaults: têm de ser escolhidos.
    day_multiplier      numeric(7, 4) not null,
    night_multiplier    numeric(7, 4) not null,
    rest_day_multiplier numeric(7, 4) not null,
    legal_basis         varchar(500),
    active              boolean       not null default true,
    created_at          timestamp     not null,
    updated_at          timestamp,
    created_by          varchar(255)
);

-- "Que acréscimos vigoram nesta data?" — a pergunta que cada recibo com horas extra faz.
create index if not exists idx_overtime_rate_configs_period
    on overtime_rate_configs (company_id, active, effective_from, effective_to);
