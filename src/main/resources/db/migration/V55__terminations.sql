-- RH B3: a saída do colaborador passa a ser um documento, não uma mudança de texto.
-- Ver docs/RH_COMPLETO_SPEC.md §B3.
--
-- Até aqui, cessar era `changeEmployeeStatus(id, "TERMINATED")` — uma String. Nada mais acontecia.
-- E o que se perdia era dinheiro que o sistema JÁ SABIA CALCULAR e nunca calculava neste contexto:
-- 13.º proporcional, férias vencidas e não gozadas, saldos por liquidar. Isso era feito à mão, em
-- papel — ou não era feito de todo, e ninguém dava por isso até o trabalhador reclamar.

create table if not exists terminations (
    id                   bigserial primary key,
    company_id           bigint         not null references companies (id),
    employee_id          bigint         not null references employees (id),
    -- Cessa-se um CONTRATO, não um "estado". Nulo só para quem saiu sem contrato registado
    -- (colaboradores anteriores ao B1) — esses cessam-se na mesma, e isso fica dito.
    contract_id          bigint         references employment_contracts (id),
    settlement_number    varchar(40)    not null,
    termination_date     date           not null,
    reason               varchar(30)    not null,
    -- Aviso prévio cumprido. Falso desconta no acerto, mas SÓ quando a saída é por iniciativa do
    -- trabalhador — e só se o número de dias estiver configurado (§6).
    notice_served        boolean        not null default true,
    total_earnings       numeric(19, 2) not null,
    total_deductions     numeric(19, 2) not null,
    net_amount           numeric(19, 2) not null,
    status               varchar(20)    not null,   -- POR_PAGAR | PAGO
    payment_date         date,
    notes                varchar(500),
    created_at           timestamp      not null,
    updated_at           timestamp,
    created_by           varchar(255)
);

-- Um colaborador cessa-se uma vez. Duas cessações seriam dois acertos finais para a mesma saída.
create unique index if not exists uk_terminations_employee
    on terminations (company_id, employee_id);

create unique index if not exists uk_terminations_number
    on terminations (company_id, settlement_number);

-- As linhas do acerto. Gravadas, ao contrário dos totais do ponto, porque um acerto é um DOCUMENTO:
-- o que foi acordado e pago naquele dia não pode mudar quando a tabela de férias mudar amanhã.
-- Mesma razão pela qual a fatura fotografa o preço e a InvoiceLine fotografa o custo (V37).
create table if not exists termination_settlement_lines (
    id              bigserial primary key,
    termination_id  bigint         not null references terminations (id) on delete cascade,
    description     varchar(200)   not null,
    amount          numeric(19, 2) not null,
    -- Verdadeiro = ganho a pagar; falso = desconto a abater.
    earning         boolean        not null,
    line_order      integer        not null,
    created_at      timestamp      not null,
    updated_at      timestamp,
    created_by      varchar(255)
);

create index if not exists idx_termination_lines
    on termination_settlement_lines (termination_id, line_order);
