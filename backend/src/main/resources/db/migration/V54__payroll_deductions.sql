-- RH B6: os descontos do recibo deixam de ser um número anónimo.
-- Ver docs/RH_COMPLETO_SPEC.md §B6.
--
-- Até aqui havia um único campo `other_deductions` — um valor solto por recibo, sem dizer o que é.
-- Numa loja em Moçambique isto é o dia-a-dia: adiantamento a meio do mês, quebra de caixa,
-- sindicato, seguro, empréstimo em prestações. E o adiantamento era o pior: SAÍA DA CAIXA E NUNCA
-- VOLTAVA, porque nada o ligava ao recibo do período.

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. O compromisso
-- ─────────────────────────────────────────────────────────────────────────────
-- UMA tabela para os três casos, e não três tabelas: um empréstimo é um adiantamento em N
-- prestações, e um desconto recorrente é um empréstimo sem capital. O que muda entre eles é só se o
-- dinheiro saiu da caixa antes e quantas vezes se desconta.
create table if not exists payroll_deductions (
    id                  bigserial primary key,
    company_id          bigint         not null references companies (id),
    employee_id         bigint         not null references employees (id),
    kind                varchar(20)    not null,   -- ADIANTAMENTO | EMPRESTIMO | RECORRENTE
    description         varchar(200)   not null,
    -- Capital em dívida no início. Nulo num desconto recorrente sem fim (sindicato, seguro):
    -- esse não tem capital, tem vigência.
    principal_amount    numeric(19, 2),
    installment_amount  numeric(19, 2) not null,
    -- Nulo = enquanto estiver activo e dentro da vigência.
    installments        integer,
    start_date          date           not null,
    end_date            date,
    -- O dinheiro já saiu da tesouraria (adiantamento e empréstimo saem; recorrente não).
    paid_out            boolean        not null default false,
    active              boolean        not null default true,
    notes               varchar(500),
    created_at          timestamp      not null,
    updated_at          timestamp,
    created_by          varchar(255)
);

create index if not exists idx_payroll_deductions_employee
    on payroll_deductions (company_id, employee_id, active);

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. O que cada recibo levou de facto
-- ─────────────────────────────────────────────────────────────────────────────
-- O SALDO EM DÍVIDA NÃO É GRAVADO — apura-se destas linhas. Mesma lição da caducidade da cotação e
-- dos totais do ponto: um saldo gravado é uma segunda verdade, e desactualiza-se à primeira
-- anulação de recibo. Anular um recibo apaga as suas linhas, e a dívida volta a existir sozinha.
create table if not exists payslip_deduction_lines (
    id            bigserial primary key,
    payslip_id    bigint         not null references payslips (id) on delete cascade,
    deduction_id  bigint         not null references payroll_deductions (id),
    description   varchar(200)   not null,
    amount        numeric(19, 2) not null,
    created_at    timestamp      not null,
    updated_at    timestamp,
    created_by    varchar(255)
);

-- Um desconto só é aplicado uma vez por recibo. Sem isto, reprocessar um recibo cobrava a mesma
-- prestação duas vezes ao colaborador — e o empréstimo ficava pago mais depressa do que devia.
create unique index if not exists uk_payslip_deduction_lines
    on payslip_deduction_lines (payslip_id, deduction_id);

create index if not exists idx_payslip_deduction_lines_deduction
    on payslip_deduction_lines (deduction_id);
