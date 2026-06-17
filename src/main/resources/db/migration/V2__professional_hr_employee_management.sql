-- Professional HR foundation for existing PostgreSQL installations.
ALTER TABLE IF EXISTS employees ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE IF EXISTS employees ADD COLUMN IF NOT EXISTS employee_number VARCHAR(30);
ALTER TABLE IF EXISTS employees ADD COLUMN IF NOT EXISTS phone VARCHAR(40);
ALTER TABLE IF EXISTS employees ADD COLUMN IF NOT EXISTS tax_id VARCHAR(40);
ALTER TABLE IF EXISTS employees ADD COLUMN IF NOT EXISTS inss_number VARCHAR(40);
ALTER TABLE IF EXISTS employees ADD COLUMN IF NOT EXISTS hire_date DATE;
ALTER TABLE IF EXISTS employees ADD COLUMN IF NOT EXISTS contract_end_date DATE;
ALTER TABLE IF EXISTS employees ADD COLUMN IF NOT EXISTS employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE employees
SET company_id = (SELECT MIN(id) FROM companies)
WHERE company_id IS NULL;

UPDATE employees
SET employee_number = 'EMP-' || LPAD(id::text, 5, '0')
WHERE employee_number IS NULL OR employee_number = '';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employees')
       AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'companies')
       AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_employees_company') THEN
        ALTER TABLE employees
            ADD CONSTRAINT fk_employees_company FOREIGN KEY (company_id) REFERENCES companies(id);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'employees')
       AND NOT EXISTS (SELECT 1 FROM employees WHERE company_id IS NULL) THEN
        ALTER TABLE employees ALTER COLUMN company_id SET NOT NULL;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_employees_company_number
    ON employees(company_id, employee_number);
CREATE UNIQUE INDEX IF NOT EXISTS ux_employees_company_email
    ON employees(company_id, email);
CREATE INDEX IF NOT EXISTS ix_employees_company_status
    ON employees(company_id, employment_status);
