-- V17 — Sincroniza o schema Flyway com as entidades JPA.
--
-- Durante o desenvolvimento o desktop corria com Hibernate ddl-auto=update, pelo que algumas
-- alterações às entidades nunca chegaram a ter migração correspondente. Esta migração fecha esse
-- desvio, permitindo voltar ao modelo de produção (Flyway dono do schema + Hibernate em validate).
-- Conteúdo derivado do diff do Hibernate (ddl-auto=update) contra o schema V1..V16.

-- 1) Colunas em falta — fluxo de aprovação de transferências de stock
ALTER TABLE stock_transfers ADD COLUMN approved_at      timestamp(6);
ALTER TABLE stock_transfers ADD COLUMN approved_by      varchar(255);
ALTER TABLE stock_transfers ADD COLUMN rejection_reason varchar(500);

-- 2) Precisão numérica alinhada com BigDecimal (numeric(38,2))
ALTER TABLE purchase_order_lines ALTER COLUMN line_total TYPE numeric(38,2);
ALTER TABLE purchase_order_lines ALTER COLUMN quantity   TYPE numeric(38,2);
ALTER TABLE purchase_order_lines ALTER COLUMN tax_rate   TYPE numeric(38,2);
ALTER TABLE purchase_order_lines ALTER COLUMN unit_price TYPE numeric(38,2);
ALTER TABLE purchase_orders      ALTER COLUMN tax_amount   TYPE numeric(38,2);
ALTER TABLE purchase_orders      ALTER COLUMN total_amount TYPE numeric(38,2);

-- 3) Restrições UNIQUE declaradas nas entidades mas ausentes nas migrações
ALTER TABLE employees       ADD CONSTRAINT uk_employees_company_employee_number UNIQUE (company_id, employee_number);
ALTER TABLE employees       ADD CONSTRAINT uk_employees_company_email           UNIQUE (company_id, email);
ALTER TABLE payroll_bonuses ADD CONSTRAINT uk_payroll_bonuses_emp_type_year_ref UNIQUE (employee_id, bonus_type, ref_year, reference_id);
ALTER TABLE payslips        ADD CONSTRAINT uk_payslips_emp_year_month           UNIQUE (employee_id, ref_year, ref_month);
ALTER TABLE product_batches ADD CONSTRAINT uk_product_batches_prod_wh_batch     UNIQUE (product_id, warehouse_id, batch_number);
ALTER TABLE stocks          ADD CONSTRAINT uk_stocks_product_warehouse          UNIQUE (product_id, warehouse_id);
ALTER TABLE tax_rates       ADD CONSTRAINT uk_tax_rates_code                     UNIQUE (code);
