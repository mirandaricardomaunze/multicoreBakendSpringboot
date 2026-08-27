create table if not exists occupational_health_exams (
    id               bigserial primary key,
    company_id       bigint        not null references companies (id),
    employee_id      bigint        not null references employees (id),
    card_number      varchar(80),
    exam_date        date          not null,
    expiry_date      date          not null,
    fitness_result   varchar(30)   not null,
    clinic           varchar(160),
    doctor_name      varchar(160),
    restrictions     varchar(1000),
    notes            varchar(1000),
    attachment_name  varchar(255),
    attachment_data  bytea,
    created_at       timestamp     not null,
    updated_at       timestamp,
    created_by       varchar(255)
);

create index if not exists idx_occupational_health_employee
    on occupational_health_exams (company_id, employee_id, exam_date desc);
create index if not exists idx_occupational_health_expiry
    on occupational_health_exams (company_id, expiry_date);
