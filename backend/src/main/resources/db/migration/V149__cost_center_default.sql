-- Centro de custo por omissão por empresa (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1, CC-03).
--
-- Cada empresa recebe o centro manual ADM — Administração, marcado como centro por omissão (usado nos
-- lançamentos sem armazém). O gestor pode depois escolher outro centro activo.
ALTER TABLE cost_centers ADD COLUMN IF NOT EXISTS default_center BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO cost_centers (company_id, code, name, type, active, default_center, created_at, created_by)
SELECT c.id, 'ADM', 'Administração', 'MANUAL', TRUE, FALSE, CURRENT_TIMESTAMP, 'system'
FROM companies c
WHERE NOT EXISTS (
    SELECT 1 FROM cost_centers cc WHERE cc.company_id = c.id AND UPPER(cc.code) = 'ADM'
);

UPDATE cost_centers
SET default_center = TRUE
WHERE UPPER(code) = 'ADM'
  AND NOT EXISTS (
      SELECT 1 FROM cost_centers d WHERE d.company_id = cost_centers.company_id AND d.default_center = TRUE
  );
