alter table payslips
    add column if not exists absence_deduction numeric(14, 2) not null default 0;
