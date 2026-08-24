-- RH: o contrato de trabalho passa a ser um documento, e passa a mandar na folha.
-- Ver docs/RH_COMPLETO_SPEC.md §B1.
--
-- Antes disto, a folha mensal filtrava só por employees.employment_status = 'ACTIVE': um colaborador
-- cujo contrato terminou a 31 de Julho continuava a receber recibo em Agosto — em silêncio, com
-- saída de tesouraria e tudo. Quem não fechasse o contrato à mão pagava a quem já não trabalhava lá.
--
-- Um colaborador tem N contratos ao longo do tempo (renovações, mudanças de função) mas UM SÓ
-- vigente numa data. A invariante é imposta no serviço (ensureNoOverlap), porque "sobreposição de
-- intervalos" não se exprime num índice único — o índice abaixo serve a consulta, não a regra.
--
-- EXPIRADO não é estado gravado: deriva-se de end_date contra hoje. Mesma lição da caducidade da
-- cotação (V4x) — gravá-lo obrigaria a um agendador nocturno e deixaria linhas desactualizadas.

create table if not exists employment_contracts (
    id                  bigserial primary key,
    contract_number     varchar(40)    not null,
    employee_id         bigint         not null references employees (id),
    company_id          bigint         not null references companies (id),
    contract_type       varchar(20)    not null,
    status              varchar(20)    not null default 'RASCUNHO',
    start_date          date           not null,
    -- Nulo em contrato sem termo e em termo incerto: o termo incerto acaba quando a tarefa acaba.
    end_date            date,
    probation_end_date  date,
    agreed_salary       numeric(19, 2) not null,
    weekly_hours        integer        not null default 40,
    job_title           varchar(120)   not null,
    work_location       varchar(200),
    -- Justificação do termo. Obrigatória em contrato a termo — exigência da lei laboral.
    term_reason         varchar(500),
    -- Renovação aponta para o contrato anterior: o histórico do que foi acordado é imutável.
    renewed_from_id     bigint         references employment_contracts (id),
    termination_date    date,
    termination_reason  varchar(500),
    created_at          timestamp      not null,
    updated_at          timestamp,
    created_by          varchar(255)
);

-- O número é único por empresa, como todas as séries gapless do sistema.
create unique index if not exists uk_employment_contracts_company_number
    on employment_contracts (company_id, contract_number);

-- "Qual é o contrato deste colaborador nesta data?" — a pergunta que a folha mensal faz por cada
-- colaborador, todos os meses. Sem este índice é varrimento de tabela vezes o número de pessoas.
create index if not exists idx_employment_contracts_employee_dates
    on employment_contracts (company_id, employee_id, status, start_date, end_date);

-- "Que contratos acabam nos próximos 30 dias?" — alimenta os alertas de fim de contrato.
create index if not exists idx_employment_contracts_ending
    on employment_contracts (company_id, status, end_date);
