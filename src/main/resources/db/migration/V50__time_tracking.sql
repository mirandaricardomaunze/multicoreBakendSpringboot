-- RH: ponto e assiduidade. As horas extra do recibo passam a ter origem.
-- Ver docs/RH_COMPLETO_SPEC.md §B2.
--
-- Hoje o campo `overtime` do recibo é um número que quem processa a folha escreve à mão: ninguém
-- sabe de onde veio, ninguém o pode contestar, e o mesmo valor pode ser pago duas vezes sem que
-- nada o note. Estas tabelas constroem a origem — marcações datadas, com autor e proveniência.
--
-- Nota: a integração com relógio biométrico continua fora (a coluna `source` já prevê 'TERMINAL').

-- Horário: é o que transforma marcações em assiduidade. Sem ele, "trabalhou 6 horas" não diz nada.
create table if not exists work_schedules (
    id                     bigserial primary key,
    company_id             bigint       not null references companies (id),
    name                   varchar(100) not null,
    monday_hours           numeric(5, 2) not null default 8.00,
    tuesday_hours          numeric(5, 2) not null default 8.00,
    wednesday_hours        numeric(5, 2) not null default 8.00,
    thursday_hours         numeric(5, 2) not null default 8.00,
    friday_hours           numeric(5, 2) not null default 8.00,
    -- Zero = dia de descanso, e é isso que torna especial a hora trabalhada nele.
    saturday_hours         numeric(5, 2) not null default 0.00,
    sunday_hours           numeric(5, 2) not null default 0.00,
    expected_start_time    time         not null default '08:00',
    late_tolerance_minutes integer      not null default 10,
    -- Janela nocturna como DADO e não constante no código: varia com a convenção aplicável, e o
    -- valor a usar tem de ser confirmado com o contabilista da empresa (§6 da spec).
    night_start            time         not null default '20:00',
    night_end              time         not null default '06:00',
    created_at             timestamp    not null,
    updated_at             timestamp,
    created_by             varchar(255)
);

create unique index if not exists uk_work_schedules_company_name
    on work_schedules (company_id, lower(name));

create table if not exists time_entries (
    id            bigserial primary key,
    employee_id   bigint       not null references employees (id),
    company_id    bigint       not null references companies (id),
    entry_date    date         not null,
    check_in      time         not null,
    check_out     time         not null,
    break_minutes integer      not null default 0,
    -- MANUAL | IMPORTADO | TERMINAL. Uma marcação escrita à mão por um chefe e uma vinda de um
    -- terminal não merecem a mesma confiança, e quem audita precisa de as distinguir.
    source        varchar(20)  not null default 'MANUAL',
    recorded_by   varchar(120) not null,
    notes         varchar(300),
    created_at    timestamp    not null,
    updated_at    timestamp,
    created_by    varchar(255)
);

-- Anti-duplicação. A spec pede único por colaborador+data+TURNO; não há modelo de turnos nesta
-- iteração, pelo que a chave é colaborador+data. Quando os turnos entrarem, este índice muda.
create unique index if not exists uk_time_entries_employee_date
    on time_entries (company_id, employee_id, entry_date);

-- "Que marcações houve neste mês?" — a consulta que o apuramento faz sempre.
create index if not exists idx_time_entries_period
    on time_entries (company_id, entry_date);

-- O mês de ponto: guarda o ESTADO do período, nunca os totais. Os totais apuram-se das marcações,
-- que são a origem; gravá-los criaria uma segunda verdade que se desactualiza à primeira correcção.
create table if not exists time_sheets (
    id               bigserial primary key,
    company_id       bigint       not null references companies (id),
    year_reference   integer      not null,
    month_reference  integer      not null,
    status           varchar(20)  not null default 'ABERTA',
    closed_by        varchar(120),
    closed_at        timestamp,
    -- Um fecho que se desfaz sem explicação não é um fecho.
    reopen_reason    varchar(300),
    created_at       timestamp    not null,
    updated_at       timestamp,
    created_by       varchar(255)
);

create unique index if not exists uk_time_sheets_company_period
    on time_sheets (company_id, year_reference, month_reference);
