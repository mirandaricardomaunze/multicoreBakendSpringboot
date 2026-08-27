-- Numeração de recibos de salário (payslips) única POR EMPRESA.
--
-- Mesmo bug multi-tenant dos documentos comerciais (ver V31): payslip_number é gerado por empresa
-- (DocumentNumberService.next(PAYSLIP), scoped à empresa activa), mas a coluna tinha UNIQUE GLOBAL e a
-- tabela NÃO tinha company_id — pelo que duas empresas que cheguem ao mesmo número colidiam.
--
-- Correcção: adicionar company_id (= empresa do colaborador, via employees) e trocar UNIQUE(payslip_number)
-- por UNIQUE(company_id, payslip_number).

alter table payslips add column if not exists company_id bigint;

-- Backfill: a empresa do recibo é a do colaborador.
update payslips p
   set company_id = e.company_id
  from employees e
 where e.id = p.employee_id
   and p.company_id is null;

alter table payslips alter column company_id set not null;

alter table payslips drop constraint if exists fk_payslips_company;
alter table payslips
    add constraint fk_payslips_company foreign key (company_id) references companies(id);

-- Trocar a UNIQUE global pela composta (empresa, número).
alter table payslips drop constraint if exists payslips_payslip_number_key;
alter table payslips drop constraint if exists uk_payslips_company_number;
alter table payslips
    add constraint uk_payslips_company_number unique (company_id, payslip_number);
