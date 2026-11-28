-- Centro de custo automático por armazém (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.1, CC-02).
--
-- Cada armazém existente recebe o seu centro WAREHOUSE: código = número do armazém quando está livre na empresa
-- (e não se repete entre armazéns), senão ARM-<id>; nome e estado acompanham o armazém. Daqui em diante é a
-- aplicação que cria/actualiza o centro sempre que um armazém é gravado.
CREATE UNIQUE INDEX IF NOT EXISTS uk_cost_centers_warehouse ON cost_centers (warehouse_id);

INSERT INTO cost_centers (company_id, code, name, type, warehouse_id, active, default_center, created_at, created_by)
SELECT w.company_id,
       CASE
           WHEN w.number_code = ''
                OR w.number_rank > 1
                OR EXISTS (SELECT 1 FROM cost_centers cc
                           WHERE cc.company_id = w.company_id AND UPPER(cc.code) = UPPER(w.number_code))
               THEN 'ARM-' || CAST(w.id AS VARCHAR(18))
           ELSE w.number_code
       END,
       SUBSTRING(w.name FROM 1 FOR 120),
       'WAREHOUSE',
       w.id,
       w.active,
       FALSE,
       CURRENT_TIMESTAMP,
       'system'
FROM (
    SELECT wh.id, wh.company_id, wh.name, wh.active,
           SUBSTRING(TRIM(COALESCE(wh.warehouse_number, '')) FROM 1 FOR 20) AS number_code,
           ROW_NUMBER() OVER (
               PARTITION BY wh.company_id, UPPER(SUBSTRING(TRIM(COALESCE(wh.warehouse_number, '')) FROM 1 FOR 20))
               ORDER BY wh.id
           ) AS number_rank
    FROM warehouses wh
) w
WHERE NOT EXISTS (SELECT 1 FROM cost_centers c WHERE c.warehouse_id = w.id);
