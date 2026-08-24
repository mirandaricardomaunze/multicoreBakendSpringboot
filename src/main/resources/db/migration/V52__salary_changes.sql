-- RH: o salário passa a ser uma série datada, não um número que se sobrepõe.
-- Ver docs/RH_COMPLETO_SPEC.md §B4.
--
-- Até aqui, updateEmployee escrevia por cima de employees.base_salary: o valor anterior não ficava
-- em lado nenhum, nem quem o mudou, nem porquê, nem desde quando. É o mesmo defeito que a V37
-- corrigiu na margem histórica do comercial, transposto para os salários.
--
-- A consequência séria não é o histórico perdido — é o RECIBO DE MARÇO passar a pagar ao valor de
-- Setembro quando alguém o reprocessa. E ninguém repara, porque o número parece normal.

create table if not exists salary_changes (
    id              bigserial primary key,
    employee_id     bigint         not null references employees (id),
    company_id      bigint         not null references companies (id),
    -- Nulo só na primeira alteração de um colaborador.
    previous_salary numeric(19, 2),
    new_salary      numeric(19, 2) not null,
    -- O campo que carrega o bloco: sem ele o passado não é reconstituível.
    effective_date  date           not null,
    reason          varchar(20)    not null,
    -- Alteração de função/departamento vive na mesma tabela, com a mesma data de efeito.
    job_title       varchar(120),
    department      varchar(120),
    approved_by     varchar(120)   not null,
    notes           varchar(500),
    created_at      timestamp      not null,
    updated_at      timestamp,
    created_by      varchar(255)
);

-- Uma alteração por colaborador e data de efeito: duas no mesmo dia tornariam ambígua a pergunta
-- "quanto ganhava esta pessoa nesse dia?", que é a única razão de esta tabela existir.
create unique index if not exists uk_salary_changes_employee_date
    on salary_changes (company_id, employee_id, effective_date);

-- "Quanto ganhava esta pessoa em Março?" — a consulta que cada recibo faz.
create index if not exists idx_salary_changes_lookup
    on salary_changes (company_id, employee_id, effective_date desc);

-- Nota: employees.base_salary continua a existir e passa a ser REFLEXO da série (o valor vigente
-- hoje), não a origem dela. A ficha deixa de o poder alterar directamente.
